/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2021
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

import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.parser.SDBLParser;
import org.antlr.v4.runtime.tree.ParseTree;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  tags = {
    DiagnosticTag.SQL,
    DiagnosticTag.STANDARD,
    DiagnosticTag.PERFORMANCE
  },
  scope = DiagnosticScope.BSL
)
public class JoinWithSubQueryDiagnostic extends AbstractSDBLVisitorDiagnostic {
  @Override
  public ParseTree visitDataSources(SDBLParser.DataSourcesContext ctx) {
    ctx.dataSource().stream()
      .filter(dataSourceContext -> !dataSourceContext.joinPart().isEmpty())
      .filter(dataSourceContext -> dataSourceContext.inlineSubquery() != null)
      .forEach(dataSourceContext -> diagnosticStorage.addDiagnostic(dataSourceContext.inlineSubquery()));

    return super.visitDataSources(ctx);
  }

  @Override
  public ParseTree visitJoinPart(SDBLParser.JoinPartContext ctx) {
    if (ctx.dataSource() != null && ctx.dataSource().inlineSubquery() != null) {
      diagnosticStorage.addDiagnostic(ctx.dataSource().inlineSubquery());
    }
    return super.visitJoinPart(ctx);
  }
}
