package nl.utwente.student.transformers

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
import nl.utwente.student.models.SupportedLanguage
import nl.utwente.student.utils.Log
import nl.utwente.student.utils.getDepth
import nl.utwente.student.utils.getUniqueName
import nl.utwente.student.visitor.java.JavaLexer
import nl.utwente.student.visitor.java.JavaParser
import nl.utwente.student.visitor.java.JavaParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import java.io.File
import java.io.FileInputStream

class JavaTransformer(override val inputFile: File) :
    JavaParserBaseVisitor<Any?>(), Transformer {
    override val language: SupportedLanguage = SupportedLanguage.JAVA
    private var imports: MutableList<String>? = null
    private var currentModuleRoot: ModuleRoot? = null
    private var currentModule: Module? = null

    override fun transform(): List<ModuleRoot> {
        val parseTree: ParseTree = FileInputStream(inputFile).use {
            val input = CharStreams.fromStream(it)
            val tokens = CommonTokenStream(JavaLexer(input))
            JavaParser(tokens).compilationUnit()
        }

        return (this.visit(parseTree) as List<*>).filterIsInstance<ModuleRoot>()
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

    private fun getSourceMetadata(token: Token): Metadata {
        val sourceMetadata = Metadata()

        sourceMetadata.startLine = token.line.toBigInteger()
        sourceMetadata.endLine = token.line.toBigInteger()
        sourceMetadata.startOffset = token.startIndex.toBigInteger()
        sourceMetadata.endOffset = token.stopIndex.toBigInteger()

        return sourceMetadata
    }

    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext?): List<ModuleRoot>? {
        // Save all imports for later use in
        ctx!!.importDeclaration()
            ?.mapNotNull(this::visitImportDeclaration)
            ?.also {
                imports = mutableListOf<String>().also { list -> list.addAll(it) }
            }

        val modules = ctx.typeDeclaration()?.mapNotNull { this.visitTypeDeclaration(it) }

        this.visitPackageDeclaration(ctx.packageDeclaration())?.let { packageName ->
            modules?.forEach { it.componentName = packageName }
        }

        return modules
    }

    override fun visitImportDeclaration(ctx: JavaParser.ImportDeclarationContext?): String? {
        return this.visitQualifiedName(ctx?.qualifiedName())
            ?.let { if (ctx?.text?.contains("*") == true) "$it.*" else it }
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

    override fun visitTypeDeclaration(ctx: JavaParser.TypeDeclarationContext?): ModuleRoot {
        val moduleRoot = ModuleRoot()
        currentModuleRoot = moduleRoot
        moduleRoot.filePath = inputFile.absolutePath
        moduleRoot.fileName = inputFile.name
        moduleRoot.module = (super.visitTypeDeclaration(ctx) as Module?)?.also {
            it.metadata = getSourceMetadata(ctx!!)
        }
        imports?.let { moduleRoot.imports.addAll(it) }

        this.visitModuleModifierList(ctx?.classOrInterfaceModifier())
            ?.let { moduleRoot.module?.modifiers?.addAll(it) }

        return moduleRoot
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

    override fun visitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?): Module {
        val module = Module()
        currentModule = module
        module.metadata = ctx?.let { getSourceMetadata(it) }
        module.id = this.visitIdentifier(ctx?.identifier())
        module.returnType = module.id
        module.moduleType = ModuleType.CLASS

        ctx?.EXTENDS()?.let {
            module.extensions.add(this.visitTypeType(ctx.typeType()))
        }

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList().first()).let { module.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.classBody()?.classBodyDeclaration())
            ?.let { module.members.addAll(it) }

        return module
    }

    override fun visitRecordDeclaration(ctx: JavaParser.RecordDeclarationContext?): Module {
        val module = Module()
        currentModule = module
        module.metadata = ctx?.let { getSourceMetadata(it) }
        module.id = this.visitIdentifier(ctx?.identifier())
        module.returnType = module.id
        module.moduleType = ModuleType.RECORD

        val unit = Unit().addMetadata(ctx)
        unit.id = module.id + ".constructor"
        unit.returnType = module.returnType

        ctx?.recordHeader()?.recordComponentList()?.recordComponent()
            ?.map { this.visitRecordComponent(it) }
            ?.also { unit.addParameters(it) }
        module.members.add(unit)

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList()).let { module.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.recordBody()?.classBodyDeclaration())
            ?.let { module.members.addAll(it) }

        return module
    }

    override fun visitInterfaceDeclaration(ctx: JavaParser.InterfaceDeclarationContext?): Module {
        val module = Module()
        currentModule = module
        module.metadata = ctx?.let { getSourceMetadata(it) }
        module.id = this.visitIdentifier(ctx?.identifier())
        module.returnType = module.id
        module.moduleType = ModuleType.INTERFACE

        ctx?.EXTENDS()?.let {
            this.visitTypeList(ctx.typeList().first()).let { module.extensions.addAll(it) }
        }

        this.visitInterfaceBodyDeclarations(ctx?.interfaceBody()?.interfaceBodyDeclaration())
            ?.let { module.members.addAll(it) }

        return module
    }

    override fun visitEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?): Module {
        val module = Module()
        currentModule = module
        module.metadata = ctx?.let { getSourceMetadata(it) }
        module.id = this.visitIdentifier(ctx?.identifier())
        module.returnType = module.id
        module.moduleType = ModuleType.ENUM

        ctx?.enumConstants()?.enumConstant()
            ?.map { this.visitEnumConstant(it) }
            ?.onEach { it.returnType = module.returnType }
            ?.also { module.members.addAll(it) }

        ctx?.IMPLEMENTS()?.let {
            this.visitTypeList(ctx.typeList()).let { module.implementations.addAll(it) }
        }

        this.visitClassBodyDeclarations(ctx?.enumBodyDeclarations()?.classBodyDeclaration())
            ?.let { module.members.addAll(it) }

        return module
    }

    override fun visitEnumConstant(ctx: JavaParser.EnumConstantContext?): Property {
        val enumConstant = Property().addMetadata(ctx)
        enumConstant.id = this.visitIdentifier(ctx?.identifier())
        // TODO(Do something with the optional class body? It extends the outer enum class)
        // TODO(How to handle the expressionList of enum constants? Can an enum even be seen as 1 Property, or multiple?)

        return enumConstant
    }

    override fun visitRecordComponent(ctx: JavaParser.RecordComponentContext?): Property {
        val property = Property().addMetadata(ctx)
        property.id = this.visitIdentifier(ctx?.identifier())
        property.returnType = this.visitTypeType(ctx?.typeType())

        return property
    }

    override fun visitConstDeclaration(ctx: JavaParser.ConstDeclarationContext?): List<Property>? {
        return ctx?.constantDeclarator()
            ?.mapNotNull(this::visitConstantDeclarator)
            ?.onEach { it.returnType = this.visitTypeType(ctx.typeType()) }
    }

    override fun visitConstantDeclarator(ctx: JavaParser.ConstantDeclaratorContext?): Property {
        return Property().also {
            it.addMetadata(ctx)
            it.id = this.visitIdentifier(ctx?.identifier())
            it.initializer = this.visitVariableInitializer(ctx?.variableInitializer())
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
            it.id = this.visitIdentifier(bodyCtx?.identifier())
            it.returnType = this.visitTypeTypeOrVoid(bodyCtx?.typeTypeOrVoid())
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

    override fun visitTypeTypeOrVoid(ctx: JavaParser.TypeTypeOrVoidContext?): String? {
        return ctx?.typeType()?.let { this.visitTypeType(it) }
    }

    override fun visitClassOrInterfaceType(ctx: JavaParser.ClassOrInterfaceTypeContext?): String? {
        return ctx?.text
    }

    override fun visitPrimitiveType(ctx: JavaParser.PrimitiveTypeContext?): String? {
        return ctx?.text
    }

    override fun visitIdentifier(ctx: JavaParser.IdentifierContext?): String? {
        return ctx?.text
    }

    private fun visitClassBodyDeclarations(ctxList: List<JavaParser.ClassBodyDeclarationContext>?): Collection<Declarable>? {
        return ctxList
            ?.mapNotNull { this.visitClassBodyDeclaration(it) }
            ?.flatten()
    }

    private fun visitInterfaceBodyDeclarations(ctxList: List<JavaParser.InterfaceBodyDeclarationContext>?): Collection<Declarable>? {
        return ctxList
            ?.mapNotNull(this::visitInterfaceBodyDeclaration)
    }

    override fun visitClassBodyDeclaration(ctx: JavaParser.ClassBodyDeclarationContext?): List<Declarable>? {
        val modifiers = this.visitModifiers(ctx?.modifier()) ?: emptyList()

        return if (ctx?.block() != null) {
            val initializer = Unit().addMetadata(ctx)
            initializer.id = "${currentModule!!.id}.initializer"
            ctx.STATIC()?.let { initializer.modifiers.add(ModifierType.STATIC) }
            initializer.body = this.visitBlock(ctx.block())
            initializer.modifiers.addAll(modifiers)
            listOf(initializer)
        } else if (ctx?.memberDeclaration() != null) {
            val members = when (val moduleMember = super.visitClassBodyDeclaration(ctx)) {
                is List<*> -> moduleMember.filterIsInstance<Declarable>()
                is Declarable -> listOf(moduleMember)
                else -> null
            }

            members?.forEach {
                it.modifiers.addAll(modifiers)
            }

            members
        } else {
            null // in case of ';'
        }
    }

    override fun visitInterfaceBodyDeclaration(ctx: JavaParser.InterfaceBodyDeclarationContext?): Declarable? {
        return if (ctx?.interfaceMemberDeclaration() == null) null else {
            val scope: Declarable? =
                this.visitInterfaceMemberDeclaration(ctx.interfaceMemberDeclaration()) as? Declarable
            // TODO(Check if this is valid, seeing how property objects are returned in field declarations.)

            val modifiers = this.visitModifiers(ctx.modifier())
            if (modifiers?.isEmpty() == true) {
                scope?.modifiers?.addAll(modifiers)
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
        method.id = this.visitIdentifier(ctx?.identifier())
        method.returnType = this.visitTypeTypeOrVoid(ctx?.typeTypeOrVoid())
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
                p.id = id
                p.returnType = visitTypeType(ctx?.receiverParameter()?.typeType())
            })
        }

        ctx?.formalParameterList()?.let {
            this.visitFormalParameterList(it)
                ?.let { l -> list.addAll(l) }
        }

        return list
    }

    override fun visitReceiverParameter(ctx: JavaParser.ReceiverParameterContext?): String? {
        if (ctx == null) return null

        val idList = mutableListOf<String>()
        ctx.identifier()?.mapNotNull { this.visitIdentifier(it) }?.forEach { idList.add(it) }
        idList.add("this")

        return idList.joinToString(".")
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

    override fun visitMethodBody(ctx: JavaParser.MethodBodyContext?): Expression? {
        return super.visitMethodBody(ctx) as? Expression
    }

    override fun visitBlock(ctx: JavaParser.BlockContext?): Expression? {
        if (ctx == null) return null
        return visitBlockStatements(ctx, ctx.blockStatement())
    }

    private fun visitBlockStatements(
        ctx: ParserRuleContext?,
        ctxList: List<JavaParser.BlockStatementContext>?
    ): Expression? {
        if (ctx == null || ctxList == null) return null

        val body = Expression().addMetadata(ctx).also { it.context = "java:Block" }

        ctxList.mapNotNull(this::visitBlockStatement).flatten().filterNotNull().let {
            body.innerScope.addAll(it)
        }

        return body
    }

    override fun visitBlockStatement(ctx: JavaParser.BlockStatementContext?): List<SourceElement?>? {
        return when {
            ctx == null -> null
            ctx.localVariableDeclaration() != null -> this.visitLocalVariableDeclaration(ctx.localVariableDeclaration())
            ctx.localTypeDeclaration() != null -> listOf(this.visitLocalTypeDeclaration(ctx.localTypeDeclaration()))
            ctx.statement() != null -> listOf(this.visitStatement(ctx.statement()))
            else -> null
        }
    }

    override fun visitConstructorDeclaration(ctx: JavaParser.ConstructorDeclarationContext?): Unit {
        val constructor = Unit().addMetadata(ctx)
        constructor.id = currentModule?.id + ".constructor"
        constructor.returnType = currentModule?.returnType
        constructor.body = this.visitBlock(ctx?.block())

        this.visitFormalParameters(ctx?.formalParameters()).let { constructor.addParameters(it) }

        return constructor
    }

    override fun visitGenericConstructorDeclaration(ctx: JavaParser.GenericConstructorDeclarationContext?): Unit {
        return this.visitConstructorDeclaration(ctx?.constructorDeclaration())
    }

    override fun visitFieldDeclaration(ctx: JavaParser.FieldDeclarationContext?): List<Property> {
        val properties = mutableListOf<Property>()
        this.visitVariableDeclarators(ctx?.variableDeclarators())
            ?.onEach {
                it.returnType = this.visitTypeType(ctx?.typeType())
                properties.add(it)
            }

        return properties
    }

    override fun visitVariableDeclarators(ctx: JavaParser.VariableDeclaratorsContext?): List<Property>? {
        return ctx?.variableDeclarator()
            ?.mapNotNull(this::visitVariableDeclarator)
    }

    override fun visitVariableDeclarator(ctx: JavaParser.VariableDeclaratorContext?): Property {
        val property = Property().addMetadata(ctx)
        property.id = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())
        property.initializer = this.visitVariableInitializer(ctx?.variableInitializer())

        return property
    }

    override fun visitVariableDeclaratorId(ctx: JavaParser.VariableDeclaratorIdContext?): String? {
        return this.visitIdentifier(ctx?.identifier())
    }

    override fun visitVariableInitializer(ctx: JavaParser.VariableInitializerContext?): Expression? {
        return if (ctx?.expression() != null) {
            this.visitExpression(ctx.expression())
        } else {
            this.visitArrayInitializer(ctx?.arrayInitializer())
        }?.addMetadata(ctx)
    }

    private fun visitParameter(
        ctx: ParserRuleContext?,
        idCtx: JavaParser.VariableDeclaratorIdContext?,
        modifierCtx: List<JavaParser.VariableModifierContext>?,
        typeCtx: JavaParser.TypeTypeContext?
    ): Property? {
        if (ctx == null) return null

        return Property().also {
            it.addMetadata(ctx)
            it.id = this.visitVariableDeclaratorId(idCtx)
            it.returnType = this.visitTypeType(typeCtx)
            this.visitModifiers(modifierCtx)?.let { m -> it.modifiers.addAll(m) }
        }
    }

    override fun visitFormalParameter(ctx: JavaParser.FormalParameterContext?): Property? {
        return visitParameter(ctx, ctx?.variableDeclaratorId(), ctx?.variableModifier(), ctx?.typeType())
    }

    override fun visitLastFormalParameter(ctx: JavaParser.LastFormalParameterContext?): Property? {
        return visitParameter(ctx, ctx?.variableDeclaratorId(), ctx?.variableModifier(), ctx?.typeType())
    }

    override fun visitLocalVariableDeclaration(ctx: JavaParser.LocalVariableDeclarationContext?): List<SourceElement>? {
        val modifiers = this.visitModifiers(ctx?.variableModifier())

        return if (ctx?.VAR() != null) {
            val varDeclaration = Property().addMetadata(ctx)
            // FIXME: Can we infer the type of this var?

            varDeclaration.id = this.visitIdentifier(ctx.identifier())
            varDeclaration.initializer = this.visitExpression(ctx.expression())
            modifiers?.let { varDeclaration.modifiers.addAll(it) }

            listOf(varDeclaration)
        } else {
            val declarations = this.visitVariableDeclarators(ctx?.variableDeclarators())?.map {
                it.also { p -> p.returnType = this.visitTypeType(ctx?.typeType()) }
            }

            return declarations
        }
    }

    override fun visitLocalTypeDeclaration(ctx: JavaParser.LocalTypeDeclarationContext?): Declarable? {
        val module = super.visitLocalTypeDeclaration(ctx) as? Module

        // TODO Fix local enum declaration, it is broken atm.

        return module?.let {
            this.visitModuleModifierList(ctx?.classOrInterfaceModifier())?.let { m -> it.modifiers.addAll(m) }
            it
        }
    }

    /**
     * @return List<Expression>?
     */
    override fun visitStatement(ctx: JavaParser.StatementContext?): SourceElement? {
        if (ctx == null) return null

        val expression: Expression? = when {
            ctx.blockLabel != null -> {
                return visitBlock(ctx.block())?.let {
                    if (it.innerScope?.isEmpty() == true) {
                        null
                    } else if (it.innerScope?.size == 1) {
                        it.innerScope[0]
                    } else {
                        Expression().also { exp ->
                            exp.addMetadata(ctx.block())
                            exp.context = "java:BlockStatement"
                            exp.innerScope.add(it)
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
                    it.innerScope.add(this.visitExpression(ctx.expression()?.last()))
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

        val elseIf = conditional.elseExpr?.innerScope?.firstOrNull() as? Conditional
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
        return this.visitExpression(ctx?.expression())?.also {
            it.metadata = getSourceMetadata(ctx!!)
        }
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
        this.visitForInit(ctx?.forInit())?.forEach { forControl.add(it) }
        this.visitExpression(ctx?.expression())?.let { forControl.add(it) }
        this.visitExpressionList(ctx?.expressionList())?.forEach { forControl.add(it) }

        return forControl
    }

    override fun visitForInit(ctx: JavaParser.ForInitContext?): List<SourceElement?>? {
        return when {
            ctx?.localVariableDeclaration() != null -> this.visitLocalVariableDeclaration(ctx.localVariableDeclaration())
            ctx?.expressionList() != null -> this.visitExpressionList(ctx.expressionList())
            else -> null
        }
    }

    override fun visitEnhancedForControl(ctx: JavaParser.EnhancedForControlContext?): Expression? {
        if (ctx == null) return null

        val forControl = Expression().addMetadata(ctx).also { it.context = "java:EnhancedForControl" }
        val property = Property().addMetadata(ctx)
        this.visitModifiers(ctx.variableModifier())?.let { property.modifiers.addAll(it) }
        property.id = this.visitVariableDeclaratorId(ctx.variableDeclaratorId())
        property.returnType = this.visitTypeType(ctx.typeType()) // FIXME: Look if we can infer the type of VAR

        forControl.add(property)
        this.visitExpression(ctx.expression())?.let { e -> forControl.add(e) }

        return forControl
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
        this.visitBlock(ctx.block())?.innerScope?.let { tryBlock.innerScope.addAll(it) } // TODO Document: flattening applied here.
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
            it.id = this.visitIdentifier(ctx?.identifier())
            it.returnType = this.visitCatchType(ctx?.catchType())
        }

        return Catch().also {
            it.addMetadata(ctx)
            it.context = "java:CatchClause"
            it.exception = exception
            this.visitBlock(ctx?.block())?.innerScope?.let { expressions -> it.innerScope.addAll(expressions) } // TODO Document: flattening applied here.

        }
    }

    override fun visitCatchType(ctx: JavaParser.CatchTypeContext?): String? {
        return this.visitQualifiedName(ctx?.qualifiedName()?.firstOrNull())  // FIXME: catch type can be a union of multiple types. How to handle that?
    }

    override fun visitFinallyBlock(ctx: JavaParser.FinallyBlockContext?): Expression? {
        return this.visitBlock(ctx?.block())?.let { scope ->
            Expression().also {
                it.addMetadata(ctx)
                it.context = "java:FinallyBlock"
                it.addAll(scope.innerScope)
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
        this.visitBlock(ctx.block())?.also {
            this.visitResourceSpecification(ctx.resourceSpecification())?.let { declarations ->
                it.innerScope.addAll(0, declarations)
            }
        }?.innerScope?.let { tryBlock.innerScope.addAll(it) } // TODO Document: flattening applied here.

        tryWithResources.add(tryBlock)

        ctx.catchClause()
            .map { this.visitCatchClause(it) }
            .let { tryWithResources.addAll(it) }

        this.visitFinallyBlock(ctx.finallyBlock()).let { tryWithResources.add(it) }

        return tryWithResources
    }

    override fun visitResourceSpecification(ctx: JavaParser.ResourceSpecificationContext?): List<Property>? {
        return this.visitResources(ctx?.resources())
    }

    override fun visitResources(ctx: JavaParser.ResourcesContext?): List<Property>? {
        return ctx?.resource()?.mapNotNull(this::visitResource)
    }

    override fun visitResource(ctx: JavaParser.ResourceContext?): Property {
        val property = Property().addMetadata(ctx)
        this.visitModifiers(ctx?.variableModifier())?.let { property.modifiers.addAll(it) }

        property.id = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())
            ?: this.visitIdentifier(ctx?.identifier())
        property.id = this.visitClassOrInterfaceType(ctx?.classOrInterfaceType()) // FIXME: Inferred types here applicable.
        property.initializer = this.visitExpression(ctx?.expression())

        return property
    }

    private fun visitSwitchStatement(ctx: JavaParser.StatementContext): Switch {
        val switch = Switch().addMetadata(ctx).also { it.context = "java:SwitchStatement" }
        switch.subject = this.visitParExpression(ctx.parExpression())?.also { it.context = "java:SwitchSubject" }

        ctx.switchBlockStatementGroup()
            .map { this.visitSwitchBlockStatementGroup(it) }
            .forEach { switch.cases.addAll(it) }

        return switch
    }

    override fun visitSwitchBlockStatementGroup(ctx: JavaParser.SwitchBlockStatementGroupContext?): List<SwitchCase> {
        val cases = mutableListOf<SwitchCase>()

        ctx?.switchLabel()?.forEachIndexed { i, l ->
            this.visitSwitchLabel(l).also {
                it?.addAll(this.visitBlockStatement(ctx.blockStatement(i)))
            }?.let { cases.add(it) }
        }

        return cases
    }

    override fun visitSwitchLabel(ctx: JavaParser.SwitchLabelContext?): SwitchCase? {
        if (ctx == null) return null

        val switchCase = SwitchCase().addMetadata(ctx).also { it.context = "java:SwitchCase" }
        switchCase.pattern = when {
            ctx.constantExpression != null -> this.visitExpression(ctx.constantExpression)
                .also { it?.context = "java:ConstantSwitchLabel" }

            ctx.enumConstantName != null -> Expression().addMetadata(ctx.IDENTIFIER())
                .also { it.context = "java:EnumConstantSwitchLabel" }

            ctx.varName != null -> Expression().also {
                it.addMetadata(ctx)
                it.context = "java:DeclarationSwitchLabel"
                it.add(Property().also { p ->
                    p.addMetadata(ctx)
                    p.id = this.visitIdentifier(ctx.varName)
                    p.returnType = this.visitTypeType(ctx.typeType())
                })
            }

            else -> {
                switchCase.addMetadata(ctx.DEFAULT())
                switchCase.context = "java:DefaultSwitchCase"
                null
            }
        }

        return switchCase
    }

    override fun visitSwitchExpression(ctx: JavaParser.SwitchExpressionContext?): Switch {
        val switch = Switch().addMetadata(ctx).also { it.context = "java:SwitchExpression" }
        switch.subject = this.visitParExpression(ctx?.parExpression())
        ctx?.switchLabeledRule()
            ?.mapNotNull { this.visitSwitchLabeledRule(it) }
            ?.let { switch.cases.addAll(it) }

        return switch
    }

    override fun visitSwitchLabeledRule(ctx: JavaParser.SwitchLabeledRuleContext?): SwitchCase? {
        if (ctx == null) return null

        val switchCase = SwitchCase().addMetadata(ctx).also { it.context = "java:SwitchCase" }
        switchCase.pattern = when {
            ctx.expressionList() != null -> Expression().also {
                it.context = "java:ExpressionList"
                it.addMetadata(ctx.expressionList())
                it.addAll(this.visitExpressionList(ctx.expressionList()))
            }
            ctx.NULL_LITERAL() != null -> Expression().also {
                it.context = "java:NullLiteral"
                it.addMetadata(ctx.NULL_LITERAL())
            }
            ctx.guardedPattern() != null -> this.visitGuardedPattern(ctx.guardedPattern())
            else -> null
        }
        switchCase.innerScope.add(this.visitSwitchRuleOutcome(ctx.switchRuleOutcome()))

        return switchCase
    }

    override fun visitSwitchRuleOutcome(ctx: JavaParser.SwitchRuleOutcomeContext?): Expression? {
        return when {
            ctx?.block() != null -> this.visitBlock(ctx.block())
            ctx?.blockStatement() != null -> this.visitBlockStatements(ctx, ctx.blockStatement())
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
                    it.addMetadata(ctx)
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

            this.visitExpression(ctx?.expression()?.firstOrNull())?.let { pattern.operands.add(it) }
        }

        return pattern
    }

    private fun visitSynchronizedStatement(ctx: JavaParser.StatementContext): Expression? {
        val synchronizedStatement = this.visitParExpression(ctx.parExpression())
        synchronizedStatement?.context = "java:SynchronizedStatement"
        synchronizedStatement?.addMetadata(ctx)
        this.visitBlock(ctx.block())?.innerScope?.let { synchronizedStatement?.innerScope?.addAll(it) } // TODO Document the flattening
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
            it.label = this.visitIdentifier(ctx?.identifier())
            it.context = "java:BreakStatement"
        }
    }

    private fun visitContinueStatement(ctx: JavaParser.StatementContext?): Expression {
        return Jump().also {
            it.label = this.visitIdentifier(ctx?.identifier())
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
            ctx.bop != null && ctx.bop.text == "." -> this.visitAccessExpression(ctx)
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
            ctx.identifier() != null -> ReferenceAccess().also {
                it.addMetadata(ctx)
                it.declarableId = this.visitIdentifier(ctx.identifier())
                it.context = "java:PrimaryIdentifier"
            }

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

    private fun visitAccessExpression(ctx: JavaParser.ExpressionContext): Access {
        val prefix = this.visitExpression(ctx.expression().first())

        val access: Access = when {
            ctx.identifier() != null -> ReferenceAccess().also {
                it.context = "java:VariableAccessCall"
                it.declarableId = this.visitIdentifier(ctx.identifier())
            }

            ctx.THIS() != null -> ReferenceAccess().also {
                it.context = "java:ThisAccessCall"
                it.declarableId = "this"
            }

            ctx.methodCall() != null -> this.visitMethodCall(ctx.methodCall())
            ctx.NEW() != null -> UnitCall().also {
                // Reference to Class, a.k.a. constructor call.
                it.context = "java:ConstructorReference"

                // TODO(Document: We change the call to 'constructor' and put the identifier in the nested scope)
                it.declarableId = (prefix as? ReferenceAccess)?.declarableId + ".constructor"
                it.addAll(
                    this.createAnonymousClass(
                        ctx.innerCreator().classCreatorRest(),
                        this.visitIdentifier(ctx.innerCreator().identifier())
                    )?.innerScope
                )

                this.visitArguments(ctx.innerCreator().classCreatorRest().arguments())
                    ?.let { list -> it.arguments.addAll(list) }
            }

            ctx.SUPER() != null -> UnitCall().also {
                it.declarableId = "super"
                it.context = "SuperCall"
                // TODO(Document: We do not handle super suffix.)
            }

            ctx.explicitGenericInvocation() != null -> UnitCall().also {
                it.context = "java:ExplicitGenericInvocation"

                val obj = ctx.explicitGenericInvocation().explicitGenericInvocationSuffix()
                if (obj.SUPER() != null) {
                    // TODO(Document: We do not handle superSuffix correctly, with the possibility of super.identifier(args) )
                    it.declarableId = "super"
                    this.visitArguments(obj.superSuffix().arguments())?.let { args -> it.arguments.addAll(args) }
                } else {
                    it.declarableId = this.visitIdentifier(obj.identifier())
                    this.visitArguments(obj.arguments())?.let { args -> it.arguments.addAll(args) }
                }
            }

            else -> UnitCall()
        }.addMetadata(ctx)

        // TODO(Document: We chain the method call prefixes within the current UnitCall.)
        access.add(prefix)

        return access
    }

    private fun createAnonymousClass(
        ctx: JavaParser.ClassCreatorRestContext?,
        moduleReference: String?
    ): Expression? {
        var scope: Expression? = null

        this.visitClassBodyDeclarations(ctx?.classBody()?.classBodyDeclaration())?.let { members ->
            scope = Expression()

            // Anonymous class // TODO(Document: how this anonymous class works)
            scope!!.innerScope.add(Module().also { m ->
                m.metadata = ctx?.let { getSourceMetadata(it) }
                // TODO(Document: we have unique names for anonymous classes.
                m.id = moduleReference + ctx?.start.hashCode()
                m.returnType = moduleReference
                m.members.addAll(members)
            })
        }

        return scope
    }

    override fun visitMethodCall(ctx: JavaParser.MethodCallContext?): UnitCall {
        val call = UnitCall()
        call.context = "java:MethodCall"
        call.declarableId = when {
            ctx?.identifier() != null -> this.visitIdentifier(ctx.identifier())
            ctx?.THIS() != null -> "this"
            ctx?.SUPER() != null -> "super"
            else -> null
        }

        this.visitExpressionList(ctx?.expressionList()).let {
            it?.let { args ->
                call.arguments.addAll(args.filterIsInstance<Expression>())
            }
        }

        return call
    }


    private fun visitArrayAccessExpression(ctx: JavaParser.ExpressionContext): Expression? {
        return this.visitExpression(ctx.expression().first())?.also {
            it.context = "java:ArrayAccess"
            it.add(this.visitExpression(ctx.expression().last()))
        }
    }

    override fun visitCreator(ctx: JavaParser.CreatorContext?): ReferenceAccess? {
        val referenceAccess = this.visitCreatedName(ctx?.createdName())

        return referenceAccess?.also {
            when {
                ctx?.nonWildcardTypeArguments() != null || ctx?.classCreatorRest() != null -> {
                    referenceAccess.add(UnitCall().addMetadata(ctx).also { unit ->
                        unit.context = "java:ClassCreator"
                        this.visitArguments(ctx.classCreatorRest().arguments())
                            ?.let { args -> unit.arguments.addAll(args) }

                        // TODO(document: we did the constructor reference again.)
                        unit.declarableId = referenceAccess.declarableId + ".constructor"
                        unit.addAll(
                            createAnonymousClass(
                                ctx.classCreatorRest(),
                                referenceAccess.declarableId
                            )?.innerScope
                        )
                    })
                }

                else -> {
                    it.context = "java:ArrayCreator"
                    it.add(this.visitArrayCreatorRest(ctx?.arrayCreatorRest()))
                }
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
            Expression().also { exp ->
                exp.context = "java:ArrayInitializer"
                exp.addMetadata(ctx)
                exp.addAll(it)
            }
        }
    }

    override fun visitCreatedName(ctx: JavaParser.CreatedNameContext?): ReferenceAccess? {
        return ctx?.let {
            ReferenceAccess().also { access ->
                access.addMetadata(ctx)
                access.declarableId = ctx.identifier()?.mapNotNull { this.visitIdentifier(it) }?.joinToString(".")
                access.context = "java:AnonymousClassReference"
            }
        }
    }

    private fun visitTypeCheck(ctx: JavaParser.ExpressionContext): Expression {
        return Expression().also {
            it.context = "java:InstanceOf"
            it.add(this.visitExpression(ctx.expression().first()))
        }
    }

    private fun visitUnaryExpression(ctx: JavaParser.ExpressionContext): Expression? {
        return if (ctx.postfix?.text == "++" || ctx.postfix?.text == "--" || ctx.prefix?.text == "--" || ctx.prefix?.text == "++") {
            (this.visitExpression(ctx.expression().first()) as? ReferenceAccess)?.also {
                Assignment().also { assign ->
                    ctx.postfix?.also { t -> assign.addMetadata(t) }
                    ctx.prefix?.also { t -> assign.addMetadata(t) }
                    assign.declarableId = it.declarableId
                }
            }
        } else {
            // We ignore + - in pre and post.
            Expression().also {
                it.add(this.visitExpression(ctx.expression().first()))
            }
        }.also {
            it?.context = "java:UnaryExpression"
            it?.addMetadata(ctx)
        }
    }

    private fun visitBinaryExpression(ctx: JavaParser.ExpressionContext): Expression? {
        val leftSide = this.visitExpression(ctx.expression().first())
        val rightSide = this.visitExpression(ctx.expression().last())

        return when (ctx.bop?.text) {
            "=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", ">>=", ">>>=", "<<=", "%=" -> (leftSide as? ReferenceAccess)
                ?.also { access ->
                    access.add(Assignment().also {
                        it.declarableId = leftSide.declarableId
                        it.value = rightSide
                        it.context = "java:Assignment"
                        it.addMetadata(ctx)
                    })
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
        val lambda = Lambda().addMetadata(ctx).also {
            it.context = "java:LambdaExpression"
            it.unit = Unit().also { u ->
                u.addMetadata(ctx)
                u.id = it.getUniqueName(currentModuleRoot)
            }
        }

        this.visitLambdaParameters(ctx?.lambdaParameters()).let { lambda.unit.addParameters(it) }
        this.visitLambdaBody(ctx?.lambdaBody()).let { lambda.unit.body = it }

        return lambda
    }

    override fun visitLambdaParameters(ctx: JavaParser.LambdaParametersContext?): List<Property> {
        val params = mutableListOf<Property>()

        ctx?.identifier()?.mapNotNull { this.visitIdentifier(it) }?.forEachIndexed { i, id ->
            params.add(Property().also { p ->
                p.id = id
                // FIXME: Type is inferred
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
            it.id = this.visitIdentifier(ctx?.identifier())
            // FIXME: Type is inferred
            (this.visitModifiers(ctx?.variableModifier()))?.let { m -> it.modifiers.addAll(m) }
        }
    }

    override fun visitLambdaBody(ctx: JavaParser.LambdaBodyContext?): Expression {
        val lambdaBody = Expression().also {
            it.addMetadata(ctx)
            it.context = "java:LambdaBody"
        }

        this.visitExpression(ctx?.expression())?.let { lambdaBody.add(it) }
        this.visitBlock(ctx?.block())?.let { lambdaBody.addAll(it.innerScope) }

        return lambdaBody
    }

    // FIXME: Update method references as ReferenceAccess instead of Lambda.
    private fun visitMethodReference(ctx: JavaParser.ExpressionContext): Expression {
        // TODO(Document: How method references are handled.)
        return Lambda().also {
            it.context = "java:LambdaExpression"
            it.addMetadata(ctx)
            it.unit = Unit().also { u -> u.addMetadata(ctx) }
            it.add((when {
                // TODO (Document: How we handle references )
                ctx.expression() != null -> this.visitExpression(
                    ctx.expression().firstOrNull()
                ) as? ReferenceAccess // TODO Check if comes through correctly.
                ctx.typeType()?.isNotEmpty() == true -> ReferenceAccess().also { rf ->
                    rf.declarableId = ctx.typeType().first().text
                    rf.addMetadata(ctx.typeType().first())
                }

                ctx.classType() != null -> ReferenceAccess().also { rf ->
                    rf.declarableId = this.visitIdentifier(ctx.classType().identifier())
                    rf.addMetadata(ctx.classType())
                }

                else -> null
            })?.also { rf ->
                rf.context = "java:MethodReference"
                rf.add(UnitCall().addMetadata(ctx).also { uc ->
                    uc.declarableId =
                        if (ctx.NEW() == null) this.visitIdentifier(ctx.identifier()) else rf.declarableId + ".constructor"
                })
            })
        }
    }

    override fun visitExpressionList(ctx: JavaParser.ExpressionListContext?): List<SourceElement>? {
        if (ctx == null) {
            return null
        }

        return ctx.expression()?.mapNotNull { this.visitExpression(it) }
    }

    override fun visitArguments(ctx: JavaParser.ArgumentsContext?): List<Expression>? {
        return this.visitExpressionList(ctx?.expressionList())?.filterIsInstance<Expression>()
    }

    private fun <T : SourceElement> T.addMetadata(ctx: ParserRuleContext?): T {
        return if (ctx != null) this.also { metadata = getSourceMetadata(ctx) } else this
    }

    private fun <T : SourceElement> T.addMetadata(terminalNode: TerminalNode?): T {
        return this.addMetadata(terminalNode?.symbol)
    }

    private fun <T : SourceElement> T.addMetadata(token: Token?): T {
        return if (token != null) this.also { metadata = getSourceMetadata(token) } else this
    }

    private fun Expression.add(element: SourceElement?) {
        this.addAll(listOf(element))
    }

    private fun Expression.addAll(elements: List<SourceElement?>?) {
        elements?.filterNotNull()?.let {
            if (it.isNotEmpty()) {
                this.innerScope.addAll(it)
            }
        }
    }

    private fun Unit.addParameters(properties: List<Property?>?) {
        properties?.filterNotNull()?.let {
            if (it.isNotEmpty()) {
                this.parameters.addAll(it.filter { p -> p.id != null })
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

