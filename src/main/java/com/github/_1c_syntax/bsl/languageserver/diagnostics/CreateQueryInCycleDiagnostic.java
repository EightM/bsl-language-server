/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2019
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver.diagnostics;

import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.parser.BSLParser.AssignmentContext;
import com.github._1c_syntax.bsl.parser.BSLParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@DiagnosticMetadata(
  type = DiagnosticType.ERROR,
  severity = DiagnosticSeverity.CRITICAL,
  minutesToFix = 20,
  tags = {
    DiagnosticTag.PERFORMANCE
  }
)
public class CreateQueryInCycleDiagnostic extends AbstractVisitorDiagnostic {
  private static final Pattern EXECUTE_CALL_PATTERN = Pattern.compile(
    "Выполнить|Execute",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private static final Pattern QUERY_BUILDER_PATTERN = Pattern.compile(
    "ПостроительЗапроса|QueryBuilder",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private static final Pattern REPORT_BUILDER_PATTERN = Pattern.compile(
    "ПостроительОтчета|ReportBuilder",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private static final Pattern QUERY_PATTERN = Pattern.compile(
    "Запрос|Query",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);


  private static final String BOOLEAN_TYPE = "Boolean";
  private static final String DATE_TYPE = "Datetime";
  private static final String NULL_TYPE = "Null";
  private static final String NUMBER_TYPE = "Number";
  private static final String REPORT_BUILDER_TYPE = "ReportBuilder";
  private static final String STRING_TYPE = "String";
  private static final String QUERY_BUILDER_TYPE = "QueryBuilder";
  private static final String QUERY_TYPE = "Query";
  private static final String UNDEFINED_TYPE = "Undefined";
  private static final String GLOBAL_SCOPE = "GLOBAL_SCOPE";
  private static final String MODULE_SCOPE = "MODULE_SCOPE";

  private VariableScope currentScope;

  public CreateQueryInCycleDiagnostic(DiagnosticInfo info) {
    super(info);
  }

  @Override
  public ParseTree visitFile(BSLParser.FileContext ctx) {
    currentScope = new VariableScope();
    ParseTree result = super.visitFile(ctx);
    currentScope = null;
    return result;
  }

  @Override
  public ParseTree visitFileCodeBlock(BSLParser.FileCodeBlockContext ctx) {
    currentScope.enterScope(MODULE_SCOPE);
    ParseTree result = super.visitFileCodeBlock(ctx);
    currentScope.leaveScope();
    return result;
  }

  @Override
  public ParseTree visitProcedure(BSLParser.ProcedureContext ctx) {
    currentScope.enterScope(ctx.procDeclaration().subName().getText());
    ParseTree result = super.visitProcedure(ctx);
    currentScope.leaveScope();
    return result;
  }

  @Override
  public ParseTree visitFunction(BSLParser.FunctionContext ctx) {
    currentScope.enterScope(ctx.funcDeclaration().subName().getText());
    ParseTree result = super.visitFunction(ctx);
    currentScope.leaveScope();
    return result;
  }

  @Override
  public ParseTree visitAssignment(AssignmentContext ctx) {
    if (ctx.expression() == null) {
      return super.visitAssignment(ctx);
    }

    BSLParser.MemberContext firstMember = ctx.expression().member(0);
    if (firstMember == null) {
      return super.visitAssignment(ctx);
    }
    String variableName = ctx.lValue().getText();
    VariableDefinition currentVariable = new VariableDefinition(variableName);
    currentVariable.addDeclaration(ctx.lValue());

    if (firstMember.complexIdentifier() != null) {
      currentVariable.types.addAll(getTypesFromComplexIdentifier(firstMember.complexIdentifier()));
    } else if (firstMember.constValue() != null) {
      currentVariable.types.add(getTypeFromConstValue(firstMember.constValue()));
    } else {
      currentVariable.addType(UNDEFINED_TYPE);
    }

    currentScope.addVariable(variableName, currentVariable);
    return super.visitAssignment(ctx);
  }

  private String getTypeFromConstValue(BSLParser.ConstValueContext constValue) {
    if (constValue.string() != null) {
      return STRING_TYPE;
    } else if (constValue.DATETIME() != null) {
      return DATE_TYPE;
    } else if (constValue.numeric() != null) {
      return NUMBER_TYPE;
    } else if (constValue.TRUE() != null) {
      return BOOLEAN_TYPE;
    } else if (constValue.FALSE() != null) {
      return BOOLEAN_TYPE;
    } else if (constValue.NULL() != null) {
      return NULL_TYPE;
    } else {
      return UNDEFINED_TYPE;
    }
  }

  private Set<String> getTypesFromComplexIdentifier(BSLParser.ComplexIdentifierContext complexId) {

    if (complexId.newExpression() != null) {
      return Set.of(getTypeFromNewExpressionContext(complexId.newExpression()));
    } else if (complexId.IDENTIFIER() != null) {
      Optional<VariableDefinition> variableDefinition = currentScope.getVariableByName(getComplexPathName(complexId, null));
      if (variableDefinition.isPresent()) {
        return variableDefinition.get().types;
      } else {
        return Set.of(UNDEFINED_TYPE);
      }
    }
    return Set.of();
  }

  private String getTypeFromNewExpressionContext(BSLParser.NewExpressionContext newExpression) {

    String typeName = Optional.ofNullable(newExpression.typeName())
      .map(RuleContext::getText)
      .or(() -> Optional.ofNullable(newExpression.doCall())
        .map(BSLParser.DoCallContext::callParamList)
        .flatMap(callParamListContext -> callParamListContext.callParam().stream().findFirst())
        .map(BSLParser.CallParamContext::expression)
        .map(BSLParser.ExpressionContext::member)
        .flatMap(memberListContext -> memberListContext.stream().findFirst())
        .map(BSLParser.MemberContext::constValue)
        .filter(constValue -> getTypeFromConstValue(constValue).equals(STRING_TYPE))
        .map(constValue -> {
          String constValueText = constValue.getText();
          return constValueText.substring(1, constValueText.length() - 1);
        })
      )
      .orElse(UNDEFINED_TYPE);

    if (QUERY_BUILDER_PATTERN.matcher(typeName).matches()) {
      return QUERY_BUILDER_TYPE;
    } else if (REPORT_BUILDER_PATTERN.matcher(typeName).matches()) {
      return REPORT_BUILDER_TYPE;
    } else if (QUERY_PATTERN.matcher(typeName).matches()) {
      return QUERY_TYPE;
    } else {
      return typeName;
    }
  }

  private String getVariableNameFromCallStatementContext(BSLParser.CallStatementContext callStatement) {
    return callStatement.IDENTIFIER().getText();
  }

  private String getVariableNameFromModifierContext(BSLParser.ModifierContext modifier) {
    return getComplexPathName(((BSLParser.ComplexIdentifierContext) modifier.getParent()), modifier);
  }

  private String getComplexPathName(BSLParser.ComplexIdentifierContext ci, BSLParser.ModifierContext to) {
    StringBuilder sb = new StringBuilder();
    sb.append(ci.getChild(0).getText());
    for (BSLParser.ModifierContext mod : ci.modifier()) {
      if (mod.equals(to)) {
        break;
      }
      sb.append(mod.getText());
    }
    return sb.toString();

  }

  @Override
  public ParseTree visitAccessCall(BSLParser.AccessCallContext ctx) {
    if (!EXECUTE_CALL_PATTERN.matcher(ctx.methodCall().methodName().getText()).matches()) {
      return super.visitAccessCall(ctx);
    }
    if (!currentScope.codeFlowInCycle()) {
      return super.visitAccessCall(ctx);
    }

    String variableName = null;
    BSLParserRuleContext errorContext = null;
    BSLParserRuleContext parent = (BSLParserRuleContext) ctx.getParent();
    if (parent instanceof BSLParser.CallStatementContext) {
      errorContext = parent;
      variableName = getVariableNameFromCallStatementContext((BSLParser.CallStatementContext) parent);
    } else if (parent instanceof BSLParser.ModifierContext) {
      BSLParser.ModifierContext callModifier = (BSLParser.ModifierContext) parent;
      errorContext = (BSLParser.ComplexIdentifierContext) callModifier.getParent();
      variableName = getVariableNameFromModifierContext(callModifier);
    }
    Optional<VariableDefinition> variableDefinition = currentScope.getVariableByName(variableName);
    BSLParserRuleContext finalErrorContext = errorContext;
    if (finalErrorContext != null) {
      variableDefinition.ifPresent(e -> {

        if (e.types.contains(QUERY_BUILDER_TYPE)
          || e.types.contains(REPORT_BUILDER_TYPE)
          || e.types.contains(QUERY_TYPE)) {
          diagnosticStorage.addDiagnostic(finalErrorContext);
        }

      });
    }

    return super.visitAccessCall(ctx);
  }

  @Override
  public ParseTree visitForEachStatement(BSLParser.ForEachStatementContext ctx) {
    currentScope.flowMode.push(CodeFlowType.CYCLE);
    ParseTree result = super.visitForEachStatement(ctx);
    currentScope.flowMode.pop();
    return result;
  }

  @Override
  public ParseTree visitWhileStatement(BSLParser.WhileStatementContext ctx) {
    currentScope.flowMode.push(CodeFlowType.CYCLE);
    ParseTree result = super.visitWhileStatement(ctx);
    currentScope.flowMode.pop();
    return result;
  }

  @Override
  public ParseTree visitForStatement(BSLParser.ForStatementContext ctx) {
    currentScope.flowMode.push(CodeFlowType.CYCLE);
    ParseTree result = super.visitForStatement(ctx);
    currentScope.flowMode.pop();
    return result;
  }

  public enum CodeFlowType {
    LINEAR, CYCLE
  }

  public static class VariableDefinition {
    private String variableName;
    private Set<String> types = new HashSet<>();
    private ParseTree firstDeclaration;

    VariableDefinition(String variableName) {
      this.variableName = variableName;
    }

    public void addType(String type) {
      this.types.add(type);
    }

    public void addDeclaration(ParseTree firstDeclaration) {
      if (this.firstDeclaration == null) {
        this.firstDeclaration = firstDeclaration;
      }
    }

    @Override
    public String toString() {
      return "VariableDefinition{" +
        "variableName='" + variableName + '\'' +
        ", variableTypes=" + types +
        ", declaration=" + firstDeclaration.getText() +
        '}';
    }
  }

  private static class Scope {
    private final String name;

    private HashMap<String, VariableDefinition> variables = new HashMap<>();

    public Scope(String name) {
      this.name = name;
    }

    public void addVariable(String variableName, VariableDefinition variableDefinition, boolean typesMerge) {
      this.variables.merge(variableName, variableDefinition, (key, value) -> {
        if (!typesMerge) {
          key.types.clear();
        }
        key.types.addAll(value.types);

        return key;
      });
    }

    public String getName() {
      return name;
    }
  }

  private static class VariableScope extends ArrayDeque<Scope> {
    private Deque<CodeFlowType> flowMode = new ArrayDeque<>();

    public VariableScope() {
      this.enterScope(GLOBAL_SCOPE);
    }

    public boolean codeFlowInCycle() {
      final CodeFlowType flowType = flowMode.peek();
      if (flowType == null) {
        return false;
      }
      return flowType.equals(CodeFlowType.CYCLE);
    }

    public Optional<VariableDefinition> getVariableByName(String variableName) {
      return Optional.ofNullable(current().variables.get(variableName));
    }

    public void addVariable(String variableName, VariableDefinition variableDefinition) {
      final CodeFlowType flowType = flowMode.peek();
      if (flowType == null) {
        return;
      }
      this.current().addVariable(variableName, variableDefinition, flowType.equals(CodeFlowType.CYCLE));
    }

    public void enterScope(String name) {
      Scope newScope = new Scope(name);
      if (!this.isEmpty()) {
        Scope prevScope = this.peek();
        newScope.variables.putAll(prevScope.variables);
      }
      this.push(newScope);
      flowMode.push(CodeFlowType.LINEAR);
    }

    public void leaveScope() {
      this.pop();
      flowMode.pop();
    }

    public Scope current() {
      return this.peek();
    }

  }
}
