package nl.utwente.student.transformers

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.utils.Log
import nl.utwente.student.utils.getDepth
import nl.utwente.student.visitor.java.JavaParser
import nl.utwente.student.visitor.java.JavaParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import java.io.File

class JavaTransformer(private val javaFile: File, private val javaParseTree: ParseTree) :
    JavaParserBaseVisitor<Any?>() {
    private var imports: MutableList<String>? = null

    fun transform(): List<Module> {
        return (this.visit(javaParseTree) as List<*>).filterIsInstance<Module>()
    }

    /**
     * Metadata about the source code that needs to be added to the module, module members, statements and expressions.
     */
    private fun getSourceMetadata(ctx: ParserRuleContext): Metadata {
        val sourceMetadata = Metadata()

        sourceMetadata.startLine = ctx.start.line.toBigInteger()
        sourceMetadata.endLine = ctx.stop.line.toBigInteger()
        sourceMetadata.startOffset = ctx.start.charPositionInLine.toBigInteger()
        sourceMetadata.endOffset = ctx.stop.charPositionInLine.toBigInteger()

        return sourceMetadata
    }

//        val interval = Interval(ctx.start.startIndex, ctx.stop.stopIndex)
//        expr.code = ctx.start.inputStream.getText(interval)

    /**
     * @return List<Module>
     */
    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext?): List<Module>? {
        // Save all imports for later use in
        ctx!!.importDeclaration()
            ?.mapNotNull(this::visitImportDeclaration)
            ?.also {
                imports = mutableListOf<String>().also { list -> list.addAll(it) }
            }

        val modules = ctx.typeDeclaration()?.mapNotNull { this.visitTypeDeclaration(it) }

        this.visitPackageDeclaration(ctx.packageDeclaration())?.let { packageName ->
            modules?.forEach { it.packageName = packageName }
        }

        return modules
    }

    override fun visitImportDeclaration(ctx: JavaParser.ImportDeclarationContext?): String? {
        return this.visitQualifiedName(ctx?.qualifiedName())
    }

    override fun visitPackageDeclaration(ctx: JavaParser.PackageDeclarationContext?): String? {
        return this.visitQualifiedName(ctx?.qualifiedName())
    }

    /**
     * @return String like packages, e.g. "nl.utwente.student"
     */
    override fun visitQualifiedName(ctx: JavaParser.QualifiedNameContext?): String? {
        return ctx?.identifier()?.mapNotNull { it.text }?.joinToString(".")
    }

    override fun visitTypeDeclaration(ctx: JavaParser.TypeDeclarationContext?): Module {
        val module = Module()
        module.filePath = javaFile.absolutePath
        module.fileName = javaFile.name
        module.metadata = getSourceMetadata(ctx!!)
        module.moduleScope = super.visitTypeDeclaration(ctx) as ModuleScope?

        this.visitModuleModifierList(ctx.classOrInterfaceModifier())
            ?.let { module.moduleScope?.modifiers?.addAll(it) }

        return module
    }

    private fun visitModuleModifierList(modifiers: List<JavaParser.ClassOrInterfaceModifierContext>?): List<ModifierType>? {
        var list: List<ModifierType>? = null

        modifiers
            ?.mapNotNull { this.visitClassOrInterfaceModifier(it) }
            ?.also {
                list = mutableListOf<ModifierType>().also { list -> list.addAll(it) }
            }

        return list
    }

    override fun visitModifier(ctx: JavaParser.ModifierContext?): ModifierType? {
        return if (ctx?.classOrInterfaceModifier() != null)
            this.visitClassOrInterfaceModifier(ctx.classOrInterfaceModifier())
        else null
    }

    override fun visitVariableModifier(ctx: JavaParser.VariableModifierContext?): ModifierType? {
        return if (ctx?.FINAL() != null) ModifierType.FINAL else null
    }

    override fun visitClassOrInterfaceModifier(ctx: JavaParser.ClassOrInterfaceModifierContext?): ModifierType? {
        return try {
            ModifierType.fromValue(ctx?.text)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    override fun visitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?): ModuleScope {
        val moduleScope = ModuleScope()
        moduleScope.identifier = this.visitIdentifier(ctx?.identifier())
        moduleScope.moduleType = ModuleType.CLASS

        ctx?.EXTENDS()?.let {
            moduleScope.extensions.add(this.visitTypeType(ctx.typeType()))
        }

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList().first()).let { moduleScope.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.classBody()?.classBodyDeclaration())
            ?.let { moduleScope.members.addAll(it) }

        return moduleScope
    }

    override fun visitRecordDeclaration(ctx: JavaParser.RecordDeclarationContext?): ModuleScope {
        val moduleScope = ModuleScope()
        moduleScope.identifier = this.visitIdentifier(ctx?.identifier())
        moduleScope.moduleType = ModuleType.RECORD

        val unit = Unit().addMetadata(ctx)
        unit.identifier = createIdentifier("constructor", ctx?.identifier())

        ctx?.recordHeader()?.recordComponentList()?.recordComponent()
            ?.map { this.visitRecordComponent(it) }
            ?.also { unit.addParameters(it) }
        moduleScope.members.add(unit)

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList()).let { moduleScope.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.recordBody()?.classBodyDeclaration())
            ?.let { moduleScope.members.addAll(it) }

        return moduleScope
    }

    override fun visitInterfaceDeclaration(ctx: JavaParser.InterfaceDeclarationContext?): ModuleScope {
        val moduleScope = ModuleScope()
        moduleScope.identifier = this.visitIdentifier(ctx?.identifier())
        moduleScope.moduleType = ModuleType.INTERFACE

        ctx?.EXTENDS()?.let {
            this.visitTypeList(ctx.typeList().first()).let { moduleScope.extensions.addAll(it) }
        }

        this.visitInterfaceBodyDeclarations(ctx?.interfaceBody()?.interfaceBodyDeclaration())
            ?.let { moduleScope.members.addAll(it) }

        return moduleScope
    }

    override fun visitEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?): ModuleScope {
        val moduleScope = ModuleScope()
        moduleScope.identifier = this.visitIdentifier(ctx?.identifier())
        moduleScope.moduleType = ModuleType.ENUM

        ctx?.enumConstants()?.enumConstant()
            ?.map { this.visitEnumConstant(it) }
            ?.also { moduleScope.members.addAll(it) }

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList()).let { moduleScope.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.enumBodyDeclarations()?.classBodyDeclaration())
            ?.let { moduleScope.members.addAll(it) }

        return moduleScope
    }

    override fun visitEnumConstant(ctx: JavaParser.EnumConstantContext?): Property {
        val enumConstant = Property().addMetadata(ctx)
        enumConstant.identifier = this.visitIdentifier(ctx?.identifier())
        // TODO(Do something with the optional class body? It extends the outer enum class)
        // TODO(How to handle the expressionList of enum constants? Can an enum even be seen as 1 Property, or multiple?)

        return enumConstant
    }

    override fun visitRecordComponent(ctx: JavaParser.RecordComponentContext?): Property {
        val property = Property().addMetadata(ctx)
        property.identifier = this.visitIdentifier(ctx?.identifier())

        return property
    }

    override fun visitConstDeclaration(ctx: JavaParser.ConstDeclarationContext?): List<Property>? {
        return ctx?.constantDeclarator()?.mapNotNull(this::visitConstantDeclarator)
    }

    override fun visitConstantDeclarator(ctx: JavaParser.ConstantDeclaratorContext?): Property {
        return Property().also {
            it.addMetadata(ctx)
            it.identifier = this.visitIdentifier(ctx?.identifier())
            it.value = this.visitVariableInitializer(ctx?.variableInitializer())
        }
    }

    private fun visitInterfaceMethod(
        ctx: ParserRuleContext?,
        modifierCtx: List<JavaParser.InterfaceMethodModifierContext>?,
        bodyCtx: JavaParser.InterfaceCommonBodyDeclarationContext?
    ): Unit {
        return Unit().also {
            it.addMetadata(ctx)
            modifierCtx?.mapNotNull(this::visitInterfaceMethodModifier)?.let { m -> it.modifiers.addAll(m) }
            it.addParameters(this.visitFormalParameters(bodyCtx?.formalParameters()))
            it.identifier = this.visitIdentifier(bodyCtx?.identifier())
            it.body = this.visitMethodBody(bodyCtx?.methodBody())
        }
    }

    override fun visitInterfaceMethodDeclaration(ctx: JavaParser.InterfaceMethodDeclarationContext?): Unit {
        return visitInterfaceMethod(ctx, ctx?.interfaceMethodModifier(), ctx?.interfaceCommonBodyDeclaration())
    }

    override fun visitInterfaceMethodModifier(ctx: JavaParser.InterfaceMethodModifierContext?): ModifierType? {
        return when {
            ctx?.PUBLIC() != null -> ModifierType.PUBLIC
            ctx?.ABSTRACT() != null -> ModifierType.ABSTRACT
            ctx?.DEFAULT() != null -> ModifierType.DEFAULT
            ctx?.STATIC() != null -> ModifierType.STATIC
            else -> null
        }
    }

    override fun visitGenericInterfaceMethodDeclaration(ctx: JavaParser.GenericInterfaceMethodDeclarationContext?): Unit {
        return visitInterfaceMethod(ctx, ctx?.interfaceMethodModifier(), ctx?.interfaceCommonBodyDeclaration())
    }

    override fun visitTypeList(ctx: JavaParser.TypeListContext?): List<String> {
        val list = mutableListOf<String>()

        ctx?.typeType()?.mapNotNull { this.visitTypeType(it) }?.forEach {
            // TODO(Handle import dependencies)
            list.add(it)
        }

        return list
    }

    override fun visitTypeType(ctx: JavaParser.TypeTypeContext?): String? {
        return if (ctx?.classOrInterfaceType() != null) {
            this.visitClassOrInterfaceType(ctx.classOrInterfaceType())
        } else {
            this.visitPrimitiveType(ctx?.primitiveType())
        }
    }

    override fun visitClassOrInterfaceType(ctx: JavaParser.ClassOrInterfaceTypeContext?): String? {
        return ctx?.text
    }

    override fun visitPrimitiveType(ctx: JavaParser.PrimitiveTypeContext?): String? {
        return ctx?.text
    }

    override fun visitIdentifier(ctx: JavaParser.IdentifierContext?): Identifier? {
        return createIdentifier(ctx?.text, ctx)
    }

    private fun createIdentifier(text: String?, ctx: ParserRuleContext?): Identifier? {
        return ctx?.let {
            Identifier().addMetadata(ctx).also {
                it.value = text
                it.context = "java:Identifier"
            }
        }
    }

    private fun visitClassBodyDeclarations(ctxList: List<JavaParser.ClassBodyDeclarationContext>?): Collection<Scope>? {
        return ctxList
            ?.mapNotNull { this.visitClassBodyDeclaration(it) }
            ?.flatten()
    }

    private fun visitInterfaceBodyDeclarations(ctxList: List<JavaParser.InterfaceBodyDeclarationContext>?): Collection<Scope>? {
        return ctxList
            ?.mapNotNull(this::visitInterfaceBodyDeclaration)
    }

    override fun visitClassBodyDeclaration(ctx: JavaParser.ClassBodyDeclarationContext?): List<Scope>? {
        val modifiers = this.visitModifiers(ctx?.modifier()) ?: emptyList()

        return if (ctx?.block() != null) {
            val initializer = Unit().addMetadata(ctx)
            initializer.identifier = createIdentifier("initializer", ctx)
            ctx.STATIC()?.let { initializer.modifiers.add(ModifierType.STATIC) }
            initializer.body = this.visitBlock(ctx.block())
            initializer.modifiers.addAll(modifiers)
            listOf(initializer)
        } else if (ctx?.memberDeclaration() != null) {
            val members = when (val moduleMember = super.visitClassBodyDeclaration(ctx)) {
                is List<*> -> moduleMember.filterIsInstance<Scope>()
                is Scope -> listOf(moduleMember)
                else -> null
            }

            members?.forEach {
                when (it) {
                    is ModuleScope -> it.modifiers.addAll(modifiers)
                    is Unit -> it.modifiers.addAll(modifiers)
                    is Property -> it.modifiers.addAll(modifiers)
                }
            }

            members
        } else {
            null // in case of ';'
        }
    }

    override fun visitInterfaceBodyDeclaration(ctx: JavaParser.InterfaceBodyDeclarationContext?): Scope? {
        return if (ctx?.interfaceMemberDeclaration() == null) null else {
            val scope: Scope? = this.visitInterfaceMemberDeclaration(ctx.interfaceMemberDeclaration()) as? Scope
            // TODO(Check if this is valid, seeing how property objects are returned in field declarations.)

            val modifiers = this.visitModifiers(ctx.modifier())
            if (modifiers?.isEmpty() == true) {
                when (scope) {
                    is ModuleScope -> scope.modifiers.addAll(modifiers)
                    is Unit -> scope.modifiers.addAll(modifiers)
                    is Property -> scope.modifiers.addAll(modifiers)
                }
            }

            scope
        }
    }

    private fun visitModifiers(ctxList: List<JavaParser.ModifierContext>?): Collection<ModifierType>? {
        return ctxList?.mapNotNull(this::visitModifier)
    }

    @JvmName("visitVariableModifiers")
    private fun visitModifiers(ctxList: List<JavaParser.VariableModifierContext>?): Collection<ModifierType>? {
        return ctxList?.mapNotNull(this::visitVariableModifier)
    }

    override fun visitMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?): Unit {
        val method = Unit().addMetadata(ctx)
        method.identifier = this.visitIdentifier(ctx?.identifier())
        method.body = this.visitMethodBody(ctx?.methodBody())
        this.visitFormalParameters(ctx?.formalParameters()).let { method.addParameters(it) }

        return method
    }

    override fun visitGenericMethodDeclaration(ctx: JavaParser.GenericMethodDeclarationContext?): Unit {
        return this.visitMethodDeclaration(ctx?.methodDeclaration())
    }

    override fun visitFormalParameters(ctx: JavaParser.FormalParametersContext?): List<Property> {
        val list = mutableListOf<Property>()

        this.visitReceiverParameter(ctx?.receiverParameter()).let { id ->
            list.add(Property().also { p ->
                p.addMetadata(ctx?.receiverParameter())
                p.identifier = id
            })
        }

        ctx?.formalParameterList()?.let {
            this.visitFormalParameterList(it)
                ?.let { l -> list.addAll(l) }
        }

        return list
    }

    override fun visitReceiverParameter(ctx: JavaParser.ReceiverParameterContext?): Identifier? {
        if (ctx == null) return null

        val idList = mutableListOf<String>()
        ctx.identifier()?.mapNotNull { this.visitIdentifier(it) }?.forEach { idList.add(it.value) }
        idList.add("this")

        return createIdentifier(idList.joinToString("."), ctx)
    }

    override fun visitFormalParameterList(ctx: JavaParser.FormalParameterListContext?): List<Property>? {
        var list = ctx?.formalParameter()
            ?.mapNotNull(this::visitFormalParameter)
            ?.toMutableList()

        ctx?.lastFormalParameter()?.let {
            this.visitLastFormalParameter(it)?.also { p ->
                if (list != null) {
                    list?.add(p)
                } else {
                    list = mutableListOf<Property>().also { l -> l.add(p) }
                }
            }
        }


        return list
    }

    override fun visitMethodBody(ctx: JavaParser.MethodBodyContext?): BlockScope? {
        return super.visitMethodBody(ctx) as? BlockScope
    }

    override fun visitBlock(ctx: JavaParser.BlockContext?): BlockScope? {
        if (ctx == null) return null
        val body = BlockScope()

        ctx.blockStatement()
            ?.mapNotNull(this::visitBlockStatement)
            ?.forEach {
                body.expressions.add(it)
            }

        return body
    }

    override fun visitBlockStatement(ctx: JavaParser.BlockStatementContext?): Expression? {
        return when {
            ctx == null -> null
            ctx.localVariableDeclaration() != null -> this.visitLocalVariableDeclaration(ctx.localVariableDeclaration())
            ctx.localTypeDeclaration() != null -> this.visitLocalTypeDeclaration(ctx.localTypeDeclaration())
            ctx.statement() != null -> this.visitStatement(ctx.statement())
            else -> null
        }
    }

    /**
     * @return Unit
     */
    override fun visitConstructorDeclaration(ctx: JavaParser.ConstructorDeclarationContext?): Unit {
        val constructor = Unit().addMetadata(ctx)
        constructor.identifier = createIdentifier("constructor", ctx?.identifier())
        constructor.body = this.visitBlock(ctx?.block())

        this.visitFormalParameters(ctx?.formalParameters()).let { constructor.addParameters(it) }

        return constructor
    }

    override fun visitGenericConstructorDeclaration(ctx: JavaParser.GenericConstructorDeclarationContext?): Unit {
        return this.visitConstructorDeclaration(ctx?.constructorDeclaration())
    }

    override fun visitFieldDeclaration(ctx: JavaParser.FieldDeclarationContext?): List<Property> {
        val properties = mutableListOf<Property>()
        this.visitVariableDeclarators(ctx?.variableDeclarators())?.let { properties.addAll(it) }

        return properties
    }

    override fun visitVariableDeclarators(ctx: JavaParser.VariableDeclaratorsContext?): List<Property>? {
        return ctx?.variableDeclarator()
            ?.mapNotNull(this::visitVariableDeclarator)
    }

    override fun visitVariableDeclarator(ctx: JavaParser.VariableDeclaratorContext?): Property {
        val property = Property().addMetadata(ctx)
        property.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())
        property.value = this.visitVariableInitializer(ctx?.variableInitializer())

        return property
    }

    override fun visitVariableDeclaratorId(ctx: JavaParser.VariableDeclaratorIdContext?): Identifier? {
        return this.visitIdentifier(ctx?.identifier())
    }

    override fun visitVariableInitializer(ctx: JavaParser.VariableInitializerContext?): Expression? {
        return if (ctx?.expression() != null) {
            this.visitExpression(ctx.expression())
        } else {
            this.visitArrayInitializer(ctx?.arrayInitializer())
        }
    }

    private fun visitParameter(
        ctx: ParserRuleContext?,
        idCtx: JavaParser.VariableDeclaratorIdContext?,
        modifierCtx: List<JavaParser.VariableModifierContext>?
    ): Property? {
        if (ctx == null) return null

        return Property().also {
            it.addMetadata(ctx)
            it.identifier = this.visitVariableDeclaratorId(idCtx)
            this.visitModifiers(modifierCtx)?.let { m -> it.modifiers.addAll(m) }
        }
    }

    override fun visitFormalParameter(ctx: JavaParser.FormalParameterContext?): Property? {
        return visitParameter(ctx, ctx?.variableDeclaratorId(), ctx?.variableModifier())
    }

    override fun visitLastFormalParameter(ctx: JavaParser.LastFormalParameterContext?): Property? {
        return visitParameter(ctx, ctx?.variableDeclaratorId(), ctx?.variableModifier())
    }

    override fun visitLocalVariableDeclaration(ctx: JavaParser.LocalVariableDeclarationContext?): Expression? {
        val modifiers = this.visitModifiers(ctx?.variableModifier())

        return if (ctx?.VAR() != null) {
            val varDeclaration = Property().addMetadata(ctx)

            varDeclaration.identifier = this.visitIdentifier(ctx.identifier())
            varDeclaration.value = this.visitExpression(ctx.expression())
            modifiers?.let { varDeclaration.modifiers.addAll(it) }

            Declaration().also {
                it.addMetadata(ctx)
                it.context = "java:VariableDeclaration"
                it.value = varDeclaration
            }
        } else {
            val declarations = this.visitVariableDeclarators(ctx?.variableDeclarators())?.mapIndexed { i, p ->
                Declaration().addMetadata(ctx?.variableDeclarators()?.variableDeclarator(i))
                    .also { d ->
                        d.value = p
                        d.context = "java:VariableDeclaration"
                    }
            }

            return if ((declarations?.size ?: 0) > 1) {
                return Expression().addMetadata(ctx).also {
                    it.addAll(declarations)

                    it.context = "java:MultiVariableDeclaration"
                }
            } else {
                declarations?.get(0)
            }
        }
    }

    override fun visitLocalTypeDeclaration(ctx: JavaParser.LocalTypeDeclarationContext?): Declaration? {
        val module = super.visitLocalTypeDeclaration(ctx) as? ModuleScope

        // TODO Fix local enum declaration, it is broken atm.

        return module?.let {
            this.visitModuleModifierList(ctx?.classOrInterfaceModifier())?.let { m -> it.modifiers.addAll(m) }
            Declaration().addMetadata(ctx).also { d -> d.value = it }// Wrap the module in a Declaration
        }
    }

    /**
     * @return List<Expression>?
     */
    override fun visitStatement(ctx: JavaParser.StatementContext?): Expression? {
        if (ctx == null) return null

        val expression: Expression? = when {
            ctx.blockLabel != null -> {
                return visitBlock(ctx.block())?.let {
                    if (it.expressions?.isEmpty() == true) {
                        null
                    } else if (it.expressions?.size == 1) {
                        it.expressions[0]
                    } else {
                        Expression().also { exp ->
                            exp.addMetadata(ctx.block())
                            exp.context = "java:BlockStatement"
                            exp.nestedScope = it
                        }
                    }
                }
            }

            ctx.ASSERT() != null -> this.visitAssertStatement(ctx)
            ctx.IF() != null -> this.visitIfStatement(ctx)
            ctx.FOR() != null -> this.visitForStatement(ctx)
            ctx.WHILE() != null -> {
                if (ctx.DO() == null) this.visitWhileStatement(ctx)
                else this.visitDoWhileStatement(ctx)
            }

            ctx.TRY() != null -> {
                if (ctx.resourceSpecification() == null) this.visitTryStatement(ctx)
                else this.visitTryWithResourcesStatement(ctx)
            }

            ctx.SWITCH() != null -> this.visitSwitchStatement(ctx)
            ctx.SYNCHRONIZED() != null -> this.visitSynchronizedStatement(ctx)
            ctx.RETURN() != null -> this.visitReturnStatement(ctx)
            ctx.THROW() != null -> this.visitThrowStatement(ctx)
            ctx.BREAK() != null -> this.visitBreakStatement(ctx)
            ctx.CONTINUE() != null -> this.visitContinueStatement(ctx)
            ctx.YIELD() != null -> this.visitYieldStatement(ctx)
            ctx.statementExpression != null -> this.visitStatementExpression(ctx)
            ctx.switchExpression() != null -> this.visitSwitchExpression(ctx.switchExpression())
            ctx.identifierLabel != null -> this.visitLabeledStatement(ctx)
            else -> Expression().also { it.context = "java:UnknownStatement" }
        }

        return expression?.also {
            if (it.metadata == null) {
                it.addMetadata(ctx)
            }
        }
    }

    private fun visitAssertStatement(ctx: JavaParser.StatementContext): Expression? {
        return this.visitExpression(ctx.expression()?.first())
            ?.also {
                it.context = "java:AssertStatement"
                if (ctx.expression().size > 1) {
                    it.nestedScope = BlockScope().also { b ->
                        b.expressions.add(this.visitExpression(ctx.expression()?.last()))
                    }
                }
            }
    }

    private fun visitIfStatement(ctx: JavaParser.StatementContext): Conditional {
        val conditional = Conditional().addMetadata(ctx).also { it.context = "java:IfStatement" }

        conditional.metadata = getSourceMetadata(ctx)

        val ifExpr = this.visitParExpression(ctx.parExpression())
        ifExpr?.add(this.visitStatement(ctx.statement().first()))

        conditional.ifExpr = ifExpr

        if (ctx.ELSE() != null) {
            conditional.elseExpr = Expression().also {
                it.addMetadata(ctx.statement().last())
                it.context = "java:ElseStatement"
                it.add(this.visitStatement(ctx.statement().last()))
            }
        }

        val elseIf = conditional.elseExpr?.nestedScope?.expressions?.first() as? Conditional
        if (elseIf != null) {
            conditional.elseIfExpr.add(elseIf.ifExpr.also {
                it.context = "java:IfElseStatement"
            })
            conditional.elseIfExpr.addAll(elseIf.elseIfExpr.map { it.also { e -> e.context = "java:IfElseStatement" } })
            elseIf.elseIfExpr.removeAll(elseIf.elseIfExpr)
            conditional.elseExpr = elseIf.elseExpr
        }

        return conditional
    }

    override fun visitParExpression(ctx: JavaParser.ParExpressionContext?): Expression? {
        return this.visitExpression(ctx?.expression())
    }

    private fun visitForStatement(ctx: JavaParser.StatementContext): Loop {
        val forLoop = Loop().addMetadata(ctx).also { it.context = "java:ForStatement" }

        this.visitForControl(ctx.forControl()).let { forLoop.evaluations.add(it) }

        forLoop.add(this.visitStatement(ctx.statement()?.first()))

        return forLoop
    }

    override fun visitForControl(ctx: JavaParser.ForControlContext?): Expression {
        val forControl = Expression().addMetadata(ctx).also { it.context = "java:ForControl" }

        this.visitEnhancedForControl(ctx?.enhancedForControl()).let { forControl.add(it) }
        this.visitForInit(ctx?.forInit())?.let { forControl.add(it) }
        this.visitExpression(ctx?.expression())?.let { forControl.add(it) }

        // TODO(Document: flattening of the expression list.)
        this.visitExpressionList(ctx?.expressionList()).let { forControl.addAll(it?.nestedScope?.expressions) }

        return forControl
    }

    override fun visitForInit(ctx: JavaParser.ForInitContext?): Expression? {
        return when {
            ctx?.localVariableDeclaration() != null -> this.visitLocalVariableDeclaration(ctx.localVariableDeclaration())
            ctx?.expressionList() != null -> this.visitExpressionList(ctx.expressionList())
            else -> null
        }
    }

    override fun visitEnhancedForControl(ctx: JavaParser.EnhancedForControlContext?): Expression? {
        if (ctx == null) return null

        val declaration = Declaration().addMetadata(ctx).also { it.context = "java:EnhancedForControl" }
        val property = Property().addMetadata(ctx)
        this.visitModifiers(ctx.variableModifier())?.let { property.modifiers.addAll(it) }
        property.identifier = this.visitVariableDeclaratorId(ctx.variableDeclaratorId())
        declaration.value = property

        this.visitExpression(ctx.expression())?.let { e -> declaration.add(e) }

        return declaration
    }

    private fun visitWhileStatement(ctx: JavaParser.StatementContext): Loop {
        val whileStatement = Loop().addMetadata(ctx).also { it.context = "java:WhileStatement" }
        whileStatement.evaluations.add(this.visitParExpression(ctx.parExpression()))
        this.visitStatement(ctx.statement().firstOrNull())?.let { whileStatement.add(it) }
        return whileStatement
    }

    private fun visitDoWhileStatement(ctx: JavaParser.StatementContext): Loop {
        val whileStatement = Loop().addMetadata(ctx).also { it.context = "java:DoWhileStatement" }
        whileStatement.evaluations.add(this.visitParExpression(ctx.parExpression()))
        this.visitStatement(ctx.statement().firstOrNull())?.let { whileStatement.add(it) }
        return whileStatement
    }

    private fun visitTryStatement(ctx: JavaParser.StatementContext): Expression {
        val tryStatement = Expression().also { it.context = "java:TryStatement" }

        val tryBlock = Expression().also {
            it.context = "java:TryBlock"
            it.addMetadata(ctx.block())
        }
        tryBlock.nestedScope = this.visitBlock(ctx.block())
        tryStatement.add(tryBlock)

        ctx.catchClause()
            .map { this.visitCatchClause(it) }
            .let { tryStatement.addAll(it) }

        this.visitFinallyBlock(ctx.finallyBlock()).let { tryStatement.add(it) }

        return tryStatement
    }

    override fun visitCatchClause(ctx: JavaParser.CatchClauseContext?): Catch {
        val exception = Property().also {
            it.addMetadata(ctx)
            this.visitModifiers(ctx?.variableModifier())?.let { m -> it.modifiers.addAll(m) }
            it.identifier = this.visitIdentifier(ctx?.identifier())
        }

        return Catch().also {
            it.addMetadata(ctx)
            it.context = "java:CatchClause"
            it.exception = exception
            it.nestedScope = this.visitBlock(ctx?.block())

        }
    }

    override fun visitFinallyBlock(ctx: JavaParser.FinallyBlockContext?): Expression? {
        return this.visitBlock(ctx?.block())?.let { scope ->
            Expression().also {
                it.addMetadata(ctx)
                it.context = "java:FinallyBlock"
                it.addAll(scope.expressions)
            }
        }
    }

    private fun visitTryWithResourcesStatement(ctx: JavaParser.StatementContext): Expression {
        val tryWithResources = Expression().also { // Wrapper
            it.context = "java:TryWithResources"
        }

        val tryBlock = Expression().also {
            it.addMetadata(ctx.block())
            it.context = "java:TryBlock"
        }
        tryBlock.nestedScope = this.visitBlock(ctx.block())?.also {
            this.visitResourceSpecification(ctx.resourceSpecification())?.let { declarations ->
                it.expressions.addAll(0, declarations)
            }
        }

        tryWithResources.add(tryBlock)

        ctx.catchClause()
            .map { this.visitCatchClause(it) }
            .let { tryWithResources.addAll(it) }

        this.visitFinallyBlock(ctx.finallyBlock()).let { tryWithResources.add(it) }

        return tryWithResources
    }

    override fun visitResourceSpecification(ctx: JavaParser.ResourceSpecificationContext?): List<Declaration>? {
        return this.visitResources(ctx?.resources())
    }

    override fun visitResources(ctx: JavaParser.ResourcesContext?): List<Declaration>? {
        return ctx?.resource()?.mapNotNull(this::visitResource)
    }

    override fun visitResource(ctx: JavaParser.ResourceContext?): Declaration {
        val property = Property().addMetadata(ctx)
        this.visitModifiers(ctx?.variableModifier())?.let { property.modifiers.addAll(it) }

        property.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())
            ?: this.visitIdentifier(ctx?.identifier())
        property.value = this.visitExpression(ctx?.expression())

        return Declaration().also {
            it.addMetadata(ctx)
            it.context = "java:TryResourceDeclaration"
            it.value = property
        }
    }

    private fun visitSwitchStatement(ctx: JavaParser.StatementContext): Conditional {
        val switch = Conditional().addMetadata(ctx).also { it.context = "java:SwitchStatement" }
        switch.ifExpr = this.visitParExpression(ctx.parExpression())?.also { it.context = "java:SwitchSubject" }

        ctx.switchBlockStatementGroup()
            .map { this.visitSwitchBlockStatementGroup(it) }
            .forEach { switch.ifExpr.addAll(it) }

        return switch
    }

    override fun visitSwitchBlockStatementGroup(ctx: JavaParser.SwitchBlockStatementGroupContext?): List<Expression> {
        val rules = mutableListOf<Expression>()

        ctx?.switchLabel()?.forEachIndexed { i, l ->
            this.visitSwitchLabel(l).also {
                it?.add(this.visitBlockStatement(ctx.blockStatement(i)))
            }?.let { rules.add(it) }
        }

        return rules
    }

    override fun visitSwitchLabel(ctx: JavaParser.SwitchLabelContext?): Expression? {
        return when {
            ctx?.constantExpression != null -> this.visitExpression(ctx.constantExpression)
                .also { it?.context = "java:ConstantSwitchLabel" }

            ctx?.enumConstantName != null -> Expression().also { it.context = "java:EnumConstantSwitchLabel" }
            ctx?.varName != null -> this.visitIdentifier(ctx.varName)?.also {
                it.addMetadata(ctx)
                it.context = "java:DeclarationSwitchLabel"
            }

            else -> Expression().also { it.context = "java:DefaultSwitchLabel" } // default label
        }
    }

    override fun visitSwitchExpression(ctx: JavaParser.SwitchExpressionContext?): Conditional {
        val expression = Conditional().addMetadata(ctx).also { it.context = "java:SwitchExpression" }
        expression.ifExpr = this.visitParExpression(ctx?.parExpression())
        ctx?.switchLabeledRule()
            ?.map { this.visitSwitchLabeledRule(it) }
            ?.forEach { expression.add(it) }

        return expression
    }

    override fun visitSwitchLabeledRule(ctx: JavaParser.SwitchLabeledRuleContext?): Expression? {
        return when { // TODO(Implement switchRuleOutcome within switchExpression)
            ctx?.expressionList() != null -> this.visitExpressionList(ctx.expressionList())
            ctx?.NULL_LITERAL() != null -> Expression().also { it.context = "java:NullLiteral" }
            ctx?.guardedPattern() != null -> this.visitGuardedPattern(ctx.guardedPattern())
            else -> null
        }
    }

    override fun visitGuardedPattern(ctx: JavaParser.GuardedPatternContext?): Expression {
        var pattern = Expression().also {
            it.addMetadata(ctx)
            it.context = "java:GuardedPattern"
        }

        // Guarded pattern with parenthesis
        if (ctx?.guardedPattern() != null && ctx.expression() == null) {
            return this.visitGuardedPattern(ctx.guardedPattern())
        } else if (ctx?.typeType() != null) {
            // Guarded pattern with a type check and a logical sequence of expressions
            // TODO How to declare the variableModifier* typeType identifier?
            if (ctx.expression()?.isNotEmpty() == true) {
                val seq = LogicalSequence().also {
                    it.addMetadata(ctx)
                    it.context = "java:LogicalAndSequence"
                }
                seq.operands.add(Expression().also {
                    it.context = "java:TypePattern"
                })

                ctx.expression()
                    ?.map { this.visitExpression(it) }
                    ?.let { seq.operands.addAll(it) }

                return seq
            }
        } else {
            pattern = LogicalSequence().also {
                it.addMetadata(ctx)
                it.context = "java:LogicalAndSequence"
            }

            val guardedPattern = this.visitGuardedPattern(ctx?.guardedPattern())

            // Flatten Logical Sequence
            if (guardedPattern is LogicalSequence) {
                pattern.operands.addAll(guardedPattern.operands)
            } else {
                pattern.operands.add(guardedPattern)
            }

            this.visitExpression(ctx?.expression()?.first())?.let { pattern.operands.add(it) }
        }

        return pattern
    }

    private fun visitSynchronizedStatement(ctx: JavaParser.StatementContext): Expression? {
        val synchronizedStatement = this.visitParExpression(ctx.parExpression())
        synchronizedStatement?.context = "java:SynchronizedStatement"
        synchronizedStatement?.nestedScope = this.visitBlock(ctx.block())
        return synchronizedStatement
    }

    private fun visitReturnStatement(ctx: JavaParser.StatementContext): Expression {
        return Expression().also {
            it.context = "java:ReturnStatement"
            it.add(visitExpression(ctx.expression()?.firstOrNull()))
        }
    }

    private fun visitThrowStatement(ctx: JavaParser.StatementContext): Expression {
        return Expression().also {
            it.context = "java:ThrowStatement"
            it.add(visitExpression(ctx.expression()?.firstOrNull()))
        }
    }

    private fun visitBreakStatement(ctx: JavaParser.StatementContext?): Expression {
        return Jump().also {
            it.label = this.visitIdentifier(ctx?.identifier())?.value
            it.context = "java:BreakStatement"
        }
    }

    private fun visitContinueStatement(ctx: JavaParser.StatementContext?): Expression {
        return Jump().also {
            it.label = this.visitIdentifier(ctx?.identifier())?.value
            it.context = "java:ContinueStatement"
        }
    }

    private fun visitYieldStatement(ctx: JavaParser.StatementContext): Expression {
        return Expression().also {
            it.context = "java:YieldStatement"
            it.add(visitExpression(ctx.expression().first()))
        }
    }

    private fun visitStatementExpression(ctx: JavaParser.StatementContext): Expression? {
        return this.visitExpression(ctx.statementExpression)
    }

    private fun visitLabeledStatement(ctx: JavaParser.StatementContext): Expression {
        return Expression().also {
            it.context = "java:LabeledStatement"
            it.add(visitStatement(ctx.statement().first()))
        }
    }

    override fun visitExpression(ctx: JavaParser.ExpressionContext?): Expression? {
        if (ctx == null) return null

        val expression = when {
            ctx.bop != null && ctx.bop.text == "." -> this.visitCallExpression(ctx)
            ctx.expression().size == 2 && ctx.text.contains("[") -> this.visitArrayAccessExpression(ctx)
            ctx.INSTANCEOF() != null && ctx.typeType() != null -> this.visitExpression(ctx.expression().firstOrNull())
            ctx.postfix != null || ctx.prefix != null -> this.visitUnaryExpression(ctx)
            ctx.expression().size == 2
                    && (ctx.bop != null || (ctx.text.contains("<") || ctx.text.contains(">")))
            -> this.visitBinaryExpression(ctx)

            ctx.INSTANCEOF() != null -> this.visitTypeCheck(ctx)
            ctx.expression().size == 3 && ctx.bop.text == "?" -> this.visitConditionalExpression(ctx)
            ctx.children?.filterIsInstance<TerminalNodeImpl>()
                ?.firstOrNull()?.text == "::" -> this.visitMethodReference(ctx)

            ctx.primary() != null -> this.visitPrimary(ctx.primary())
            ctx.methodCall() != null -> this.visitMethodCall(ctx.methodCall())
            ctx.creator() != null -> this.visitCreator(ctx.creator())
            ctx.lambdaExpression() != null -> this.visitLambdaExpression(ctx.lambdaExpression())
            ctx.switchExpression() != null -> this.visitSwitchExpression(ctx.switchExpression())
            else -> Expression()
        }

        return expression?.addMetadata(ctx)?.also {
            it.context = it.context ?: "java:UnknownExpression"
        }
    }

    override fun visitPrimary(ctx: JavaParser.PrimaryContext?): Expression? {
        return when {
            ctx == null -> null
            ctx.identifier() != null -> this.visitIdentifier(ctx.identifier())
            ctx.expression() != null -> this.visitExpression(ctx.expression())
            else -> Expression().also {
                it.context = "java:Literal"
            }
        }
    }

    override fun visitLiteral(ctx: JavaParser.LiteralContext?): Expression {
        return Expression().also {
            it.context = "java:Literal"
        }
    }

    private fun visitCallExpression(ctx: JavaParser.ExpressionContext): UnitCall {
        val prefix = this.visitExpression(ctx.expression().first())

        val call: UnitCall = when {
            ctx.identifier() != null -> UnitCall().also {
                it.context = "java:VariableAccessCall"
                it.reference = this.visitIdentifier(ctx.identifier())
            }

            ctx.THIS() != null -> UnitCall().also {
                it.context = "java:ThisAccessCall"
            }

            ctx.methodCall() != null -> this.visitMethodCall(ctx.methodCall())
            ctx.NEW() != null -> UnitCall().also {
                // Reference to Class, a.k.a. constructor call.
                it.context = "java:ConstructorReference"
                it.reference = this.visitIdentifier(ctx.innerCreator().identifier())

                this.visitArguments(ctx.innerCreator().classCreatorRest().arguments())
                    ?.let { list -> it.arguments.addAll(list) }

                it.nestedScope = this.createAnonymousClass(ctx.innerCreator().classCreatorRest(), it.reference)
            }

            ctx.SUPER() != null -> UnitCall().also {
                it.reference = createIdentifier("super", ctx)
                it.context = "SuperCall"
                // TODO(Document: We do not handle super suffix.)
            }

            ctx.explicitGenericInvocation() != null -> UnitCall().also {
                it.context = "java:ExplicitGenericInvocation"
            }

            else -> UnitCall()
        }.addMetadata(ctx)

        // TODO(Document: We chain the method call prefixes within the current UnitCall.)
        call.add(prefix)

        return call
    }

    private fun createAnonymousClass(
        ctx: JavaParser.ClassCreatorRestContext?,
        moduleReference: Identifier?
    ): BlockScope? {
        var scope: BlockScope? = null

        this.visitClassBodyDeclarations(ctx?.classBody()?.classBodyDeclaration())?.let { members ->
            scope = BlockScope()

            // Anonymous class // TODO(Document: how this anonymous class works)
            scope!!.expressions.add(Declaration().addMetadata(ctx).also { d ->
                d.value = ModuleScope().also { m ->
                    m.identifier = moduleReference?.also { it.value = it.value + ctx?.start.hashCode() }
                    m.members.addAll(members)
                }
            })
        }

        return scope
    }

    override fun visitMethodCall(ctx: JavaParser.MethodCallContext?): UnitCall {
        val call = UnitCall()
        call.context = "java:MethodCall"
        call.reference = when {
            ctx?.identifier() != null -> this.visitIdentifier(ctx.identifier())
            ctx?.THIS() != null -> createIdentifier("this", ctx)
            ctx?.SUPER() != null -> createIdentifier("super", ctx)
            else -> null
        }

        this.visitExpressionList(ctx?.expressionList()).let {
            it?.nestedScope?.expressions?.let { args ->
                call.arguments.addAll(args)
            }
        }

        return call
    }


    private fun visitArrayAccessExpression(ctx: JavaParser.ExpressionContext): Expression {
        return UnitCall().also {
            it.context = "java:ArrayAccess"
            it.reference = this.visitExpression(ctx.expression().first()) as? Identifier
            it.arguments.add(this.visitExpression(ctx.expression().last()))
        }
    }

    override fun visitCreator(ctx: JavaParser.CreatorContext?): UnitCall {
        val ref = this.visitCreatedName(ctx?.createdName())
        val call = UnitCall().addMetadata(ctx)

        return when {
            ctx?.nonWildcardTypeArguments() != null || ctx?.classCreatorRest() != null
            -> call.also {
                it.context = "java:ClassCreator"
                it.reference = ref
                this.visitArguments(ctx.classCreatorRest().arguments())?.let { args -> it.arguments.addAll(args) }
                it.nestedScope = createAnonymousClass(ctx.classCreatorRest(), ref)
            }

            else -> call.also {
                it.reference = ref
                it.context = "java:ArrayCreator"
                it.add(this.visitArrayCreatorRest(ctx?.arrayCreatorRest()))
            }
        }
    }

    override fun visitArrayCreatorRest(ctx: JavaParser.ArrayCreatorRestContext?): Expression {
        val expressions = mutableListOf<Expression>()

        this.visitArrayInitializer(ctx?.arrayInitializer())?.let { expressions.add(it) }
        ctx?.expression()?.mapNotNull(this::visitExpression)?.let { expressions.addAll(it) }

        return Expression().also {
            it.context = "java:ArrayCreator"
            it.addMetadata(ctx)
            it.addAll(expressions)
        }
    }

    override fun visitArrayInitializer(ctx: JavaParser.ArrayInitializerContext?): Expression? {
        return ctx?.variableInitializer()?.mapNotNull(this::visitVariableInitializer)?.let {
            Expression().also {exp ->
                exp.context = "java:ArrayInitializer"
                exp.addMetadata(ctx)
                exp.addAll(it)
            }
        }
    }

    override fun visitCreatedName(ctx: JavaParser.CreatedNameContext?): Identifier? {
        return createIdentifier(
            ctx?.identifier()?.mapNotNull { this.visitIdentifier(it)?.value }?.joinToString("."),
            ctx
        )
    }

    private fun visitTypeCheck(ctx: JavaParser.ExpressionContext): Expression {
        return Expression().also {
            it.context = "java:InstanceOf"
            it.add(this.visitExpression(ctx.expression().first()))
        }
    }

    private fun visitUnaryExpression(ctx: JavaParser.ExpressionContext): Expression {
        return if (ctx.postfix?.text == "++" || ctx.postfix?.text == "--" || ctx.prefix?.text == "--" || ctx.prefix?.text == "++") {
            Assignment().also {
                it.addMetadata(ctx)
                it.reference = this.visitExpression(ctx.expression().first()) as? Identifier
            }
        } else {
            // We ignore + - in pre and post.
            Expression().also {
                it.add(this.visitExpression(ctx.expression().first()))
            }
        }.also {
            it.context = "java:UnaryExpression"
        }
    }

    private fun visitBinaryExpression(ctx: JavaParser.ExpressionContext): Expression {
        val leftSide = this.visitExpression(ctx.expression().first())
        val rightSide = this.visitExpression(ctx.expression().last())

        return when (ctx.bop?.text) {
            "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", ">>=", ">>>=", "<<=", "%=" -> Assignment().also {
                it.reference = leftSide as? Identifier
                it.context = "java:Assignment"
                it.addMetadata(ctx)
                it.add(rightSide)
            }

            "&&", "||" -> LogicalSequence().also {
                it.addMetadata(ctx)
                it.context = "java:Logical${if (ctx.bop.text == "&&") "And" else "Or"}Sequence"

                if (leftSide is LogicalSequence && leftSide.context == it.context) {
                    it.operands.addAll(leftSide.operands)
                    leftSide.operands.removeAll(leftSide.operands)
                } else {
                    it.operands.add(leftSide)
                }

                // Flatten the right side of the logical sequence
                if (rightSide is LogicalSequence && rightSide.context == it.context) {
                    it.operands.addAll(rightSide.operands)
                    rightSide.operands.removeAll(rightSide.operands)
                } else {
                    it.operands.add(rightSide)
                }
            }

            else -> {
                Expression().also {
                    it.addMetadata(ctx)
                    it.context = "java:BinaryExpression"
                    it.addAll(listOf(leftSide, rightSide))
                }
            }
        }
    }

    private fun visitConditionalExpression(ctx: JavaParser.ExpressionContext): Expression {
        return Conditional().also {
            it.addMetadata(ctx)
            it.context = "java:TernaryExpression"
            it.ifExpr = this.visitExpression(ctx.expression(0))
            it.ifExpr.add(this.visitExpression(ctx.expression(1)))
            it.ifExpr.add(this.visitExpression(ctx.expression(2)))
        }
    }

    override fun visitLambdaExpression(ctx: JavaParser.LambdaExpressionContext?): Lambda {
        val lambda = Lambda().addMetadata(ctx).also { it.context = "java:LambdaExpression" }
        this.visitLambdaParameters(ctx?.lambdaParameters()).let { lambda.addParameters(it) }
        this.visitLambdaBody(ctx?.lambdaBody()).let { lambda.add(it) }

        return lambda
    }

    override fun visitLambdaParameters(ctx: JavaParser.LambdaParametersContext?): List<Property> {
        val params = mutableListOf<Property>()

        ctx?.identifier()?.mapNotNull { this.visitIdentifier(it) }?.forEachIndexed { i, id ->
            params.add(Property().also { p ->
                p.identifier = id
                p.addMetadata(ctx.identifier(i))
            })
        }

        this.visitFormalParameterList(ctx?.formalParameterList())?.let { params.addAll(it) }
        this.visitLambdaLVTIList(ctx?.lambdaLVTIList()).let { params.addAll(it) }

        return params
    }

    override fun visitLambdaLVTIList(ctx: JavaParser.LambdaLVTIListContext?): List<Property> {
        val params = mutableListOf<Property>()

        ctx?.lambdaLVTIParameter()
            ?.mapNotNull { this.visitLambdaLVTIParameter(it) }
            ?.forEach { params.add(it) }

        return params
    }

    override fun visitLambdaLVTIParameter(ctx: JavaParser.LambdaLVTIParameterContext?): Property {
        return Property().also {
            it.addMetadata(ctx)
            it.identifier = this.visitIdentifier(ctx?.identifier())
            (this.visitModifiers(ctx?.variableModifier()))?.let { m -> it.modifiers.addAll(m) }
        }
    }

    override fun visitLambdaBody(ctx: JavaParser.LambdaBodyContext?): Expression {
        val lambdaBody = Expression().also {
            it.addMetadata(ctx)
            it.context = "java:LambdaBody"
        }

        this.visitExpression(ctx?.expression())?.let { lambdaBody.add(it) }
        this.visitBlock(ctx?.block())?.let { lambdaBody.addAll(it.expressions) }

        return lambdaBody
    }

    private fun visitMethodReference(ctx: JavaParser.ExpressionContext): Expression {
        return Lambda().also {
            it.context = "java:LambdaExpression"
            it.addMetadata(ctx)
            it.add(UnitCall().addMetadata(ctx).also { uc ->
                uc.reference = when {
                    ctx.expression() != null -> this.visitExpression(ctx.expression().firstOrNull()) as? Identifier // TODO How to handle this expression reference? What are the possible type outcomes?
                    ctx.typeType()?.isNotEmpty() == true -> createIdentifier(ctx.typeType().first().text, ctx.typeType().first())
                    ctx.classType() != null -> this.visitIdentifier(ctx.classType().identifier())
                    else -> createIdentifier(ctx.text, ctx)
                }
            })
        }
    }

    override fun visitExpressionList(ctx: JavaParser.ExpressionListContext?): Expression? {
        if (ctx == null) {
            return null
        }

        val expr = Expression().also {
            it.addMetadata(ctx)
            it.context = "java:ExpressionList"
        }

        ctx.expression()
            ?.mapNotNull { this.visitExpression(it) }
            ?.let { expr.addAll(it) }

        return expr
    }

    override fun visitArguments(ctx: JavaParser.ArgumentsContext?): List<Expression>? {
        return this.visitExpressionList(ctx?.expressionList())?.nestedScope?.expressions
    }

    private fun Unit.addMetadata(ctx: ParserRuleContext?): Unit {
        return if (ctx != null) this.also { metadata = getSourceMetadata(ctx) } else this
    }

    private fun Property.addMetadata(ctx: ParserRuleContext?): Property {
        return if (ctx != null) this.also { metadata = getSourceMetadata(ctx) } else this
    }

    private fun <T : Expression> T.addMetadata(ctx: ParserRuleContext?): T {
        return if (ctx != null) this.also { metadata = getSourceMetadata(ctx) } else this
    }

    private fun Expression.add(expression: Expression?) {
        this.addAll(listOf(expression))
    }

    private fun Expression.addAll(exprList: List<Expression?>?) {
        exprList?.filterNotNull()?.let {
            if (it.isNotEmpty()) {
                this.nestedScope = this.nestedScope ?: BlockScope()
                this.nestedScope.expressions.addAll(it)
            }
        }
    }

    private fun Unit.addParameters(properties: List<Property?>?) {
        properties?.filterNotNull()?.let {
            if (it.isNotEmpty()) {
                this.parameters.addAll(it.filter { p -> p.identifier != null })
            }
        }
    }

    private fun Lambda.addParameters(properties: List<Property?>?) {
        properties?.filterNotNull()?.let {
            if (it.isNotEmpty()) {
                this.parameters.addAll(it.filter { p -> p.identifier != null })
            }
        }
    }

    /**
     * START OF DEBUGGING FUNCTION OVERRIDES
     */
    override fun visit(tree: ParseTree?): Any? {
        Log.d("Visiting ${tree?.javaClass?.simpleName}")
        return super.visit(tree)
    }

    override fun visitChildren(node: RuleNode?): Any? {
        Log.d("${"\t".repeat(node?.getDepth() ?: 0)}${node?.javaClass?.simpleName}")
        return super.visitChildren(node)
    }

    override fun visitTerminal(node: TerminalNode?): Any? {
        Log.d("${"\t".repeat(node?.getDepth() ?: 0)}${node?.javaClass?.simpleName}")
        return super.visitTerminal(node)
    }

}

