// Generated from compilerv1.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link compilerv1Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface compilerv1Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(compilerv1Parser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#importStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportStmt(compilerv1Parser.ImportStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#instance}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstance(compilerv1Parser.InstanceContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#instancepush}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstancepush(compilerv1Parser.InstancepushContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#memberaccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberaccess(compilerv1Parser.MemberaccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#instanceValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceValue(compilerv1Parser.InstanceValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#importPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportPath(compilerv1Parser.ImportPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#headerPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeaderPath(compilerv1Parser.HeaderPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#headerPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeaderPart(compilerv1Parser.HeaderPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(compilerv1Parser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod(compilerv1Parser.MethodContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(compilerv1Parser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParams(compilerv1Parser.ParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(compilerv1Parser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(compilerv1Parser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(compilerv1Parser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#structBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructBody(compilerv1Parser.StructBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#classMember}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMember(compilerv1Parser.ClassMemberContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#structMember}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructMember(compilerv1Parser.StructMemberContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#print}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint(compilerv1Parser.PrintContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#nativemode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNativemode(compilerv1Parser.NativemodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#syscallStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSyscallStmt(compilerv1Parser.SyscallStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#pointer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer(compilerv1Parser.PointerContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#include}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclude(compilerv1Parser.IncludeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#returnType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnType(compilerv1Parser.ReturnTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#typeRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeRef(compilerv1Parser.TypeRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#genericType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericType(compilerv1Parser.GenericTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#functionType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionType(compilerv1Parser.FunctionTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#functionTypeArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTypeArgs(compilerv1Parser.FunctionTypeArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#macro}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacro(compilerv1Parser.MacroContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#stdin}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStdin(compilerv1Parser.StdinContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#lambda}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda(compilerv1Parser.LambdaContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#lambdaDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaDefinition(compilerv1Parser.LambdaDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#lambdaLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaLiteral(compilerv1Parser.LambdaLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(compilerv1Parser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(compilerv1Parser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(compilerv1Parser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(compilerv1Parser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#assignTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignTarget(compilerv1Parser.AssignTargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#accessKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessKey(compilerv1Parser.AccessKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInit(compilerv1Parser.InitContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(compilerv1Parser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#orExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(compilerv1Parser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#andExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(compilerv1Parser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#equalityExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpr(compilerv1Parser.EqualityExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#relationalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpr(compilerv1Parser.RelationalExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#addExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExpr(compilerv1Parser.AddExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#multExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultExpr(compilerv1Parser.MultExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(compilerv1Parser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#indexedAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexedAccess(compilerv1Parser.IndexedAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#compositeLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompositeLiteral(compilerv1Parser.CompositeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(compilerv1Parser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgs(compilerv1Parser.ArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#returnStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStmt(compilerv1Parser.ReturnStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass(compilerv1Parser.ClassContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#struct}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct(compilerv1Parser.StructContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#interface}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface(compilerv1Parser.InterfaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#inheritanceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritanceClause(compilerv1Parser.InheritanceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#inheritedType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritedType(compilerv1Parser.InheritedTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link compilerv1Parser#virtualMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVirtualMethod(compilerv1Parser.VirtualMethodContext ctx);
}