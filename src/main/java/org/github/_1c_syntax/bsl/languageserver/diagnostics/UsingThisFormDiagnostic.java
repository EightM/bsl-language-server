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
package org.github._1c_syntax.bsl.languageserver.diagnostics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import org.github._1c_syntax.bsl.parser.BSLParser;
import org.github._1c_syntax.bsl.parser.BSLParserRuleContext;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MINOR,
  scope = DiagnosticScope.BSL,
  minutesToFix = 1
)
public class UsingThisFormDiagnostic extends AbstractVisitorDiagnostic {
  private static final Pattern pattern = Pattern.compile("^(этаформа|thisform)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  @Override
  public ParseTree visitProcedure(BSLParser.ProcedureContext ctx) {
    if (needCheck(ctx.procDeclaration())) {
      return super.visitProcedure(ctx);
    }

    return ctx;
  }

  @Override
  public ParseTree visitFunction(BSLParser.FunctionContext ctx) {
    if (needCheck(ctx.funcDeclaration())) {
      return super.visitFunction(ctx);
    }

    return ctx;
  }

  private static boolean needCheck(BSLParserRuleContext declaration) {
    List<BSLParser.ParamContext> params = getParams(declaration);
    return params.size() == 0;
  }

  private static List<BSLParser.ParamContext> getParams(BSLParserRuleContext declaration) {
    BSLParser.ParamListContext paramList = declaration.getRuleContext(BSLParser.ParamListContext.class, 0);
    if(paramList == null) {
      return Collections.emptyList();
    }
    return paramList.getRuleContexts(BSLParser.ParamContext.class);
  }

  @Override
  public ParseTree visitCallStatement(BSLParser.CallStatementContext ctx) {
    if(pattern.matcher(ctx.getText()).find()) {
      diagnosticStorage.addDiagnostic(ctx.start);
    }

    return super.visitCallStatement(ctx);
  }

  @Override
  public ParseTree visitExpression(BSLParser.ExpressionContext ctx) {
    for(Token child  : ctx.getTokens()) {
      if(pattern.matcher(child.getText()).find()) {
        diagnosticStorage.addDiagnostic(child);
      }
    }

    return super.visitExpression(ctx);
  }
}
