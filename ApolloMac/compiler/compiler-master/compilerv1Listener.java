// Generated from compilerv1.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link compilerv1Parser}.
 */
public interface compilerv1Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(compilerv1Parser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(compilerv1Parser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#importStmt}.
	 * @param ctx the parse tree
	 */
	void enterImportStmt(compilerv1Parser.ImportStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#importStmt}.
	 * @param ctx the parse tree
	 */
	void exitImportStmt(compilerv1Parser.ImportStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#instance}.
	 * @param ctx the parse tree
	 */
	void enterInstance(compilerv1Parser.InstanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#instance}.
	 * @param ctx the parse tree
	 */
	void exitInstance(compilerv1Parser.InstanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#instancepush}.
	 * @param ctx the parse tree
	 */
	void enterInstancepush(compilerv1Parser.InstancepushContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#instancepush}.
	 * @param ctx the parse tree
	 */
	void exitInstancepush(compilerv1Parser.InstancepushContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#memberaccess}.
	 * @param ctx the parse tree
	 */
	void enterMemberaccess(compilerv1Parser.MemberaccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#memberaccess}.
	 * @param ctx the parse tree
	 */
	void exitMemberaccess(compilerv1Parser.MemberaccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#instanceValue}.
	 * @param ctx the parse tree
	 */
	void enterInstanceValue(compilerv1Parser.InstanceValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#instanceValue}.
	 * @param ctx the parse tree
	 */
	void exitInstanceValue(compilerv1Parser.InstanceValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#importPath}.
	 * @param ctx the parse tree
	 */
	void enterImportPath(compilerv1Parser.ImportPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#importPath}.
	 * @param ctx the parse tree
	 */
	void exitImportPath(compilerv1Parser.ImportPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#headerPath}.
	 * @param ctx the parse tree
	 */
	void enterHeaderPath(compilerv1Parser.HeaderPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#headerPath}.
	 * @param ctx the parse tree
	 */
	void exitHeaderPath(compilerv1Parser.HeaderPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#headerPart}.
	 * @param ctx the parse tree
	 */
	void enterHeaderPart(compilerv1Parser.HeaderPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#headerPart}.
	 * @param ctx the parse tree
	 */
	void exitHeaderPart(compilerv1Parser.HeaderPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(compilerv1Parser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(compilerv1Parser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#method}.
	 * @param ctx the parse tree
	 */
	void enterMethod(compilerv1Parser.MethodContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#method}.
	 * @param ctx the parse tree
	 */
	void exitMethod(compilerv1Parser.MethodContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(compilerv1Parser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(compilerv1Parser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#params}.
	 * @param ctx the parse tree
	 */
	void enterParams(compilerv1Parser.ParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#params}.
	 * @param ctx the parse tree
	 */
	void exitParams(compilerv1Parser.ParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(compilerv1Parser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(compilerv1Parser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(compilerv1Parser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(compilerv1Parser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(compilerv1Parser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(compilerv1Parser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#structBody}.
	 * @param ctx the parse tree
	 */
	void enterStructBody(compilerv1Parser.StructBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#structBody}.
	 * @param ctx the parse tree
	 */
	void exitStructBody(compilerv1Parser.StructBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#classMember}.
	 * @param ctx the parse tree
	 */
	void enterClassMember(compilerv1Parser.ClassMemberContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#classMember}.
	 * @param ctx the parse tree
	 */
	void exitClassMember(compilerv1Parser.ClassMemberContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#structMember}.
	 * @param ctx the parse tree
	 */
	void enterStructMember(compilerv1Parser.StructMemberContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#structMember}.
	 * @param ctx the parse tree
	 */
	void exitStructMember(compilerv1Parser.StructMemberContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#print}.
	 * @param ctx the parse tree
	 */
	void enterPrint(compilerv1Parser.PrintContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#print}.
	 * @param ctx the parse tree
	 */
	void exitPrint(compilerv1Parser.PrintContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#nativemode}.
	 * @param ctx the parse tree
	 */
	void enterNativemode(compilerv1Parser.NativemodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#nativemode}.
	 * @param ctx the parse tree
	 */
	void exitNativemode(compilerv1Parser.NativemodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#syscallStmt}.
	 * @param ctx the parse tree
	 */
	void enterSyscallStmt(compilerv1Parser.SyscallStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#syscallStmt}.
	 * @param ctx the parse tree
	 */
	void exitSyscallStmt(compilerv1Parser.SyscallStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#pointer}.
	 * @param ctx the parse tree
	 */
	void enterPointer(compilerv1Parser.PointerContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#pointer}.
	 * @param ctx the parse tree
	 */
	void exitPointer(compilerv1Parser.PointerContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#include}.
	 * @param ctx the parse tree
	 */
	void enterInclude(compilerv1Parser.IncludeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#include}.
	 * @param ctx the parse tree
	 */
	void exitInclude(compilerv1Parser.IncludeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#returnType}.
	 * @param ctx the parse tree
	 */
	void enterReturnType(compilerv1Parser.ReturnTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#returnType}.
	 * @param ctx the parse tree
	 */
	void exitReturnType(compilerv1Parser.ReturnTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#typeRef}.
	 * @param ctx the parse tree
	 */
	void enterTypeRef(compilerv1Parser.TypeRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#typeRef}.
	 * @param ctx the parse tree
	 */
	void exitTypeRef(compilerv1Parser.TypeRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#genericType}.
	 * @param ctx the parse tree
	 */
	void enterGenericType(compilerv1Parser.GenericTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#genericType}.
	 * @param ctx the parse tree
	 */
	void exitGenericType(compilerv1Parser.GenericTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#functionType}.
	 * @param ctx the parse tree
	 */
	void enterFunctionType(compilerv1Parser.FunctionTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#functionType}.
	 * @param ctx the parse tree
	 */
	void exitFunctionType(compilerv1Parser.FunctionTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#functionTypeArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTypeArgs(compilerv1Parser.FunctionTypeArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#functionTypeArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTypeArgs(compilerv1Parser.FunctionTypeArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#macro}.
	 * @param ctx the parse tree
	 */
	void enterMacro(compilerv1Parser.MacroContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#macro}.
	 * @param ctx the parse tree
	 */
	void exitMacro(compilerv1Parser.MacroContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#stdin}.
	 * @param ctx the parse tree
	 */
	void enterStdin(compilerv1Parser.StdinContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#stdin}.
	 * @param ctx the parse tree
	 */
	void exitStdin(compilerv1Parser.StdinContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#lambda}.
	 * @param ctx the parse tree
	 */
	void enterLambda(compilerv1Parser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#lambda}.
	 * @param ctx the parse tree
	 */
	void exitLambda(compilerv1Parser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#lambdaDefinition}.
	 * @param ctx the parse tree
	 */
	void enterLambdaDefinition(compilerv1Parser.LambdaDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#lambdaDefinition}.
	 * @param ctx the parse tree
	 */
	void exitLambdaDefinition(compilerv1Parser.LambdaDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#lambdaLiteral}.
	 * @param ctx the parse tree
	 */
	void enterLambdaLiteral(compilerv1Parser.LambdaLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#lambdaLiteral}.
	 * @param ctx the parse tree
	 */
	void exitLambdaLiteral(compilerv1Parser.LambdaLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(compilerv1Parser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(compilerv1Parser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(compilerv1Parser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(compilerv1Parser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(compilerv1Parser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(compilerv1Parser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(compilerv1Parser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(compilerv1Parser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#assignTarget}.
	 * @param ctx the parse tree
	 */
	void enterAssignTarget(compilerv1Parser.AssignTargetContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#assignTarget}.
	 * @param ctx the parse tree
	 */
	void exitAssignTarget(compilerv1Parser.AssignTargetContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#accessKey}.
	 * @param ctx the parse tree
	 */
	void enterAccessKey(compilerv1Parser.AccessKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#accessKey}.
	 * @param ctx the parse tree
	 */
	void exitAccessKey(compilerv1Parser.AccessKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#init}.
	 * @param ctx the parse tree
	 */
	void enterInit(compilerv1Parser.InitContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#init}.
	 * @param ctx the parse tree
	 */
	void exitInit(compilerv1Parser.InitContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(compilerv1Parser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(compilerv1Parser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#orExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(compilerv1Parser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#orExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(compilerv1Parser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(compilerv1Parser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(compilerv1Parser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpr(compilerv1Parser.EqualityExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpr(compilerv1Parser.EqualityExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpr(compilerv1Parser.RelationalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpr(compilerv1Parser.RelationalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#addExpr}.
	 * @param ctx the parse tree
	 */
	void enterAddExpr(compilerv1Parser.AddExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#addExpr}.
	 * @param ctx the parse tree
	 */
	void exitAddExpr(compilerv1Parser.AddExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#multExpr}.
	 * @param ctx the parse tree
	 */
	void enterMultExpr(compilerv1Parser.MultExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#multExpr}.
	 * @param ctx the parse tree
	 */
	void exitMultExpr(compilerv1Parser.MultExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(compilerv1Parser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(compilerv1Parser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#indexedAccess}.
	 * @param ctx the parse tree
	 */
	void enterIndexedAccess(compilerv1Parser.IndexedAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#indexedAccess}.
	 * @param ctx the parse tree
	 */
	void exitIndexedAccess(compilerv1Parser.IndexedAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#compositeLiteral}.
	 * @param ctx the parse tree
	 */
	void enterCompositeLiteral(compilerv1Parser.CompositeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#compositeLiteral}.
	 * @param ctx the parse tree
	 */
	void exitCompositeLiteral(compilerv1Parser.CompositeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(compilerv1Parser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(compilerv1Parser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#args}.
	 * @param ctx the parse tree
	 */
	void enterArgs(compilerv1Parser.ArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#args}.
	 * @param ctx the parse tree
	 */
	void exitArgs(compilerv1Parser.ArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void enterReturnStmt(compilerv1Parser.ReturnStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void exitReturnStmt(compilerv1Parser.ReturnStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#class}.
	 * @param ctx the parse tree
	 */
	void enterClass(compilerv1Parser.ClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#class}.
	 * @param ctx the parse tree
	 */
	void exitClass(compilerv1Parser.ClassContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#struct}.
	 * @param ctx the parse tree
	 */
	void enterStruct(compilerv1Parser.StructContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#struct}.
	 * @param ctx the parse tree
	 */
	void exitStruct(compilerv1Parser.StructContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#interface}.
	 * @param ctx the parse tree
	 */
	void enterInterface(compilerv1Parser.InterfaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#interface}.
	 * @param ctx the parse tree
	 */
	void exitInterface(compilerv1Parser.InterfaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#inheritanceClause}.
	 * @param ctx the parse tree
	 */
	void enterInheritanceClause(compilerv1Parser.InheritanceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#inheritanceClause}.
	 * @param ctx the parse tree
	 */
	void exitInheritanceClause(compilerv1Parser.InheritanceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#inheritedType}.
	 * @param ctx the parse tree
	 */
	void enterInheritedType(compilerv1Parser.InheritedTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#inheritedType}.
	 * @param ctx the parse tree
	 */
	void exitInheritedType(compilerv1Parser.InheritedTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link compilerv1Parser#virtualMethod}.
	 * @param ctx the parse tree
	 */
	void enterVirtualMethod(compilerv1Parser.VirtualMethodContext ctx);
	/**
	 * Exit a parse tree produced by {@link compilerv1Parser#virtualMethod}.
	 * @param ctx the parse tree
	 */
	void exitVirtualMethod(compilerv1Parser.VirtualMethodContext ctx);
}