package nl.utwente.student.transformers

import nl.utwente.student.metamodel.v2.*
import nl.utwente.student.model.JavaFile
import nl.utwente.student.visitor.java.JavaParser
import nl.utwente.student.visitor.java.JavaParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext

class JavaTransformerV2(private val javaFile: JavaFile) : JavaParserBaseVisitor<Any?>() {
    private var imports: ModuleReferenceList? = null

    fun transform(): List<Module> {
        return (this.visit(javaFile.parseTree) as List<*>).filterIsInstance<Module>()
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
    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext?): Any? {
        // Save all imports for later use in
        ctx!!.importDeclaration()
            ?.mapNotNull(this::visitImportDeclaration)
            ?.filterIsInstance<String>()
            ?.also {
                imports = ModuleReferenceList()
                imports?.references?.addAll(it)
            }

        val modules = ctx.typeDeclaration()
            ?.mapNotNull { this.visitTypeDeclaration(it) }
            ?.filterIsInstance<Module>()

        this.visitPackageDeclaration(ctx.packageDeclaration())?.let { packageName ->
            modules?.forEach { it.packageName = packageName.toString() }
        }

        return modules
    }

    /**
     * @return String
     */
    override fun visitImportDeclaration(ctx: JavaParser.ImportDeclarationContext?): Any? {
        return this.visitQualifiedName(ctx?.qualifiedName())
    }

    /**
     * @see visitQualifiedName
     */
    override fun visitPackageDeclaration(ctx: JavaParser.PackageDeclarationContext?): Any? {
        return this.visitQualifiedName(ctx?.qualifiedName())
    }

    /**
     * @return String like packages, e.g. "nl.utwente.student"
     */
    override fun visitQualifiedName(ctx: JavaParser.QualifiedNameContext?): Any? {
        return ctx?.identifier()?.mapNotNull { it.text }?.joinToString(".")
    }

    /**
     * @return ModuleDeclaration
     */
    override fun visitTypeDeclaration(ctx: JavaParser.TypeDeclarationContext?): Any {
        val module = Module()
        module.filePath = javaFile.file.absolutePath
        module.fileName = javaFile.file.name
        module.moduleScope = super.visitTypeDeclaration(ctx) as ModuleScope?
        module.moduleScope?.modifiers = this.visitModuleModifierList(ctx?.classOrInterfaceModifier())
        return module
    }

    /**
     * @return ModifierList?
     */
    private fun visitModuleModifierList(modifiers: List<JavaParser.ClassOrInterfaceModifierContext>?): ModifierList? {
        val list = ModifierList()

        modifiers
            ?.mapNotNull { this.visitClassOrInterfaceModifier(it) }
            ?.filterIsInstance<ModifierType>()
            ?.forEach { list.modifiers.add(it) }

        return list.value()
    }

    /**
     * @return Modifier?
     */
    override fun visitModifier(ctx: JavaParser.ModifierContext?): Any? {
        return if (ctx?.classOrInterfaceModifier() != null)
            this.visitClassOrInterfaceModifier(ctx.classOrInterfaceModifier())
        else null
    }

    /**
     * @return Modifier?
     */
    override fun visitClassOrInterfaceModifier(ctx: JavaParser.ClassOrInterfaceModifierContext?): Any? {
        return try {
            ModifierType.fromValue(ctx?.text)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    /**
     * @return Module
     */
    override fun visitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?): Any {
        val moduleScope = ModuleScope()
        moduleScope.id = this.visitIdentifier(ctx?.identifier()) as String
        moduleScope.moduleType = ModuleType.CLASS

        // TODO(Extends)

        // TODO(Implements)

        // TODO(Members)


        return moduleScope
    }

//    /**
//     * @return Module
//     */
//    override fun visitRecordDeclaration(ctx: JavaParser.RecordDeclarationContext?): Any {
//        val moduleScope = ModuleScope()
//        moduleScope.id = this.visitIdentifier(ctx?.identifier()) as String
//        moduleScope.moduleType = ModuleType.RECORD
//
//
//        return moduleScope
//    }
//
//    /**
//     * @return Constructor?
//     */
//    override fun visitRecordHeader(ctx: JavaParser.RecordHeaderContext?): Any? {
//        return this.visitRecordComponentList(ctx?.recordComponentList())
//    }
//
//    /**
//     * @return Constructor?
//     */
//    override fun visitRecordComponentList(ctx: JavaParser.RecordComponentListContext?): Any? {
//        if (ctx == null) return null
//
//        val constructor = Constructor()
//        constructor.metadata = getSourceMetadata(ctx)
//
//        constructor.isPrimary = true
//
//        constructor.parameterList = ParameterList()
//        constructor.parameterList.parameters.addAll(
//            (ctx.recordComponent()?.map(this::visitRecordComponent) as List<*>).filterIsInstance<Parameter>()
//        )
//
//        return constructor
//    }
//
//    /**
//     * @return Parameter
//     */
//    override fun visitRecordComponent(ctx: JavaParser.RecordComponentContext?): Any {
//        val parameter = Parameter()
//
//        parameter.type = this.visitTypeType(ctx?.typeType()) as String?
//        parameter.identifier = this.visitIdentifier(ctx?.identifier()) as String?
//
//        return parameter
//    }
//
//    /**
//     * @return ModuleNameList
//     */
//    override fun visitTypeList(ctx: JavaParser.TypeListContext?): Any {
//        val list = ModuleNameList()
//
//        ctx?.typeType()?.mapNotNull { this.visitTypeType(it) as String? }?.forEach {
//            list.names.add(this.imports[it] ?: it)
//        }
//
//        return list
//    }
//
//    /**
//     * @return String?
//     */
//    override fun visitTypeType(ctx: JavaParser.TypeTypeContext?): Any? {
//        return if (ctx?.classOrInterfaceType() != null) {
//            this.visitClassOrInterfaceType(ctx.classOrInterfaceType())
//        } else {
//            this.visitPrimitiveType(ctx?.primitiveType())
//        }
//    }
//
//    /**
//     * @return String?
//     */
//    override fun visitClassOrInterfaceType(ctx: JavaParser.ClassOrInterfaceTypeContext?): Any? {
//        return ctx?.text
//    }
//
//    /**
//     * @return String?
//     */
//    override fun visitPrimitiveType(ctx: JavaParser.PrimitiveTypeContext?): Any? {
//        return ctx?.text
//    }
//
    /**
     * @return String?
     */
    override fun visitIdentifier(ctx: JavaParser.IdentifierContext?): Any? {
        return ctx?.text
    }

    //
//    /**
//     * @return ModuleBody
//     */
//    override fun visitClassBody(ctx: JavaParser.ClassBodyContext?): Any {
//        val body = ModuleBody()
//
//        ctx?.classBodyDeclaration()
//            ?.map { this.visitClassBodyDeclaration(it) }
//            ?.filterIsInstance<ModuleMember>()
//            ?.forEach { body.members.add(it) }
//        return body
//    }
//
//    /**
//     * @return ModuleBody
//     */
//    override fun visitRecordBody(ctx: JavaParser.RecordBodyContext?): Any {
//        val body = ModuleBody()
//
//        ctx?.classBodyDeclaration()
//            ?.map { this.visitClassBodyDeclaration(it) }
//            ?.filterIsInstance<ModuleMember>()
//            ?.forEach { body.members.add(it) }
//
//        return body
//    }
//
//    /**
//     * @return Initializer or any other ModuleMember
//     */
//    override fun visitClassBodyDeclaration(ctx: JavaParser.ClassBodyDeclarationContext?): Any? {
//        return if (ctx?.block() != null) {
//            val initializer = Initializer()
//            initializer.metadata = getSourceMetadata(ctx)
//            initializer.isIsStatic = ctx.STATIC() != null
//            initializer.body = this.visitBlock(ctx.block()) as Statement?
//            initializer
//        } else if (ctx?.memberDeclaration() != null) {
//            var moduleMember  = super.visitClassBodyDeclaration(ctx)
//
//            if (moduleMember is Module) {
//                moduleMember = MemberModuleDeclaration().also { it.module = moduleMember as Module }
//            }
//
//            moduleMember = moduleMember as ModuleMember?
//
//            val modifierList = ModifierList()
//            ctx.modifier()
//                ?.mapNotNull { this.visitModifier(it) }
//                ?.filterIsInstance<Modifier>()
//                ?.toList()
//                ?.let { items -> modifierList.items.addAll(items) }
//            moduleMember?.modifierList = modifierList.value()
//
//            moduleMember?.metadata = getSourceMetadata(ctx)
//            moduleMember
//        } else {
//            null // in case of ';'
//        }
//    }
//
//    /**
//     * @return Function
//     */
//    override fun visitMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?): Any {
//        val method = Function()
//
//        method.type = this.visitTypeTypeOrVoid(ctx?.typeTypeOrVoid()) as String
//        method.name = this.visitIdentifier(ctx?.identifier()) as String
//
//        method.parameterList = this.visitFormalParameters(ctx?.formalParameters()) as ParameterList?
//        method.body = this.visitMethodBody(ctx?.methodBody()) as Statement?
//
//        method.metadata = getSourceMetadata(ctx!!)
//        return method
//    }
//
//    override fun visitMethodBody(ctx: JavaParser.MethodBodyContext?): Any? {
//        val body = super.visitMethodBody(ctx) as Block?
//        body?.metadata = getSourceMetadata(ctx!!)
//        return body?.value()
//    }
//
//    /**
//     * @return Body?, null if body has no statements.
//     */
//    override fun visitBlock(ctx: JavaParser.BlockContext?): Any? {
//        if (ctx == null) return null
//        val body = Block()
//
//        ctx.blockStatement()
//            .mapNotNull { this.visitBlockStatement(it) as Statement? }
//            .forEach { body.statements.add(it) }
//
//        body.metadata = getSourceMetadata(ctx)
//
//        return body.value()
//    }
//
//    /**
//     * @return String?
//     */
//    override fun visitTypeTypeOrVoid(ctx: JavaParser.TypeTypeOrVoidContext?): Any? {
//        return if (ctx?.VOID() != null) "void" else this.visitTypeType(ctx?.typeType())
//    }
//
//    /**
//     * @return Constructor
//     */
//    override fun visitConstructorDeclaration(ctx: JavaParser.ConstructorDeclarationContext?): Any {
//        val constructor = Constructor()
//
//        constructor.parameterList = this.visitFormalParameters(ctx?.formalParameters()) as ParameterList?
//        constructor.body = this.visitBlock(ctx?.block()) as Statement?
//
//        return constructor
//    }
//
//    /**
//     * @return List<Field>, because the
//     */
//    override fun visitFieldDeclaration(ctx: JavaParser.FieldDeclarationContext?): Any {
//        val field = Field()
//        field.metadata = getSourceMetadata(ctx!!)
//
//        field.type = this.visitTypeType(ctx.typeType()) as String?
//        field.items.addAll((this.visitVariableDeclarators(ctx.variableDeclarators()) as List<*>)
//            .filterIsInstance<FieldItem>()
//            .map { it })
//
//        return field
//    }
//
//    /**
//     * @return List<FieldItem>
//     */
//    override fun visitVariableDeclarators(ctx: JavaParser.VariableDeclaratorsContext?): Any? {
//        return ctx?.variableDeclarator()
//            ?.mapNotNull(this::visitVariableDeclarator)
//    }
//
//    /**
//     * @return FieldItem
//     */
//    override fun visitVariableDeclarator(ctx: JavaParser.VariableDeclaratorContext?): Any {
//        val fieldItem = FieldItem()
//
//        fieldItem.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId()) as String?
//        fieldItem.variableInitializer = this.visitVariableInitializer(ctx?.variableInitializer()) as Expression?
//
//        return fieldItem
//    }
//
//    /**
//     * @return String
//     */
//    override fun visitVariableDeclaratorId(ctx: JavaParser.VariableDeclaratorIdContext?): Any? {
//        return this.visitIdentifier(ctx?.identifier())
//    }
//
//    /**
//     * @return Expression?
//     */
//    override fun visitVariableInitializer(ctx: JavaParser.VariableInitializerContext?): Any? {
//        return if (ctx?.expression() != null) {
//            this.visitExpression(ctx.expression())
//        } else null
//    }
//
//
//    /**
//     * @return ParameterList?
//     */
//    override fun visitFormalParameters(ctx: JavaParser.FormalParametersContext?): Any? {
//        val list = ParameterList()
//
//        if (ctx?.formalParameterList() != null) {
//            list.parameters.addAll((this.visitFormalParameterList(ctx.formalParameterList()) as ParameterList).parameters)
//        }
//
//        return list.value()
//    }
//
//    /**
//     * @return ParameterList
//     */
//    override fun visitFormalParameterList(ctx: JavaParser.FormalParameterListContext?): Any? {
//        val list = ParameterList()
//
//        ctx?.formalParameter()
//            ?.mapNotNull(this::visitFormalParameter)
//            ?.filterIsInstance<Parameter>()
//            ?.let { list.parameters.addAll(it) }
//
//        this.visitLastFormalParameter(ctx?.lastFormalParameter()).let {
//            list.parameters.add(it as Parameter)
//        }
//
//        return list.value()
//    }
//
//    /**
//     * @return Parameter
//     */
//    override fun visitFormalParameter(ctx: JavaParser.FormalParameterContext?): Any {
//        val parameter = Parameter()
//
//        parameter.modifierList = visitModifierList(ctx?.variableModifier())
//        parameter.type = this.visitTypeType(ctx?.typeType()) as String?
//        parameter.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId()) as String?
//
//        return parameter
//    }
//
//    /**
//     * @return Parameter
//     */
//    override fun visitLastFormalParameter(ctx: JavaParser.LastFormalParameterContext?): Any {
//        val parameter = Parameter()
//
//        parameter.modifierList = visitModifierList(ctx?.variableModifier())
//        parameter.type = this.visitTypeType(ctx?.typeType()) as String?
//        parameter.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId()) as String?
//        parameter.isIsSpread = true
//
//        return parameter
//    }
//
//    /**
//     * @return ModifierList
//     */
//    private fun visitModifierList(ctx: List<JavaParser.VariableModifierContext>?): ModifierList? {
//        val modifierList = ModifierList()
//        ctx?.mapNotNull(this::visitVariableModifier)
//            ?.filterIsInstance<Modifier>()
//            ?.toList()
//            ?.let { items -> modifierList.items.addAll(items) }
//
//        return modifierList.value()
//    }
//
//    /**
//     * @return LocalVariableDeclaration
//     */
//    override fun visitLocalVariableDeclaration(ctx: JavaParser.LocalVariableDeclarationContext?): Any {
//        val localVariable = Field()
//
//        localVariable.metadata = getSourceMetadata(ctx!!)
//        localVariable.modifierList = this.visitModifierList(ctx.variableModifier())
//
//        if (ctx.VAR() != null) {
//            val varDeclaration = FieldItem()
//
//            localVariable.type = "var"
//            varDeclaration.identifier = this.visitIdentifier(ctx.identifier()) as String
//            varDeclaration.variableInitializer = this.visitExpression(ctx.expression()) as? Expression
//
//            localVariable.items.add(varDeclaration)
//        } else {
//            localVariable.type = this.visitTypeType(ctx.typeType()) as String
//            localVariable.items.addAll((this.visitVariableDeclarators(ctx.variableDeclarators()) as List<*>).filterIsInstance<FieldItem>())
//        }
//
//        return localVariable
//    }
//
//    /**
//     * @return LocalModuleDeclaration
//     */
//    override fun visitLocalTypeDeclaration(ctx: JavaParser.LocalTypeDeclarationContext?): Any {
//        val wrapper = LocalModuleDeclaration()
//        wrapper.module = super.visitLocalTypeDeclaration(ctx) as Module
//        wrapper.module.modifierList = this.visitModuleModifierList(ctx?.classOrInterfaceModifier())
//        wrapper.metadata = getSourceMetadata(ctx!!)
//        return wrapper
//    }
//
//    /**
//     * @return Statement?
//     */
//    override fun visitStatement(ctx: JavaParser.StatementContext?): Any? {
//        if (ctx == null) return null
//
//        val statement: Statement? = when {
//            ctx.blockLabel != null -> visitBlock(ctx.block())
//            ctx.ASSERT() != null -> this.visitAssertStatement(ctx)
//            ctx.IF() != null -> this.visitIfStatement(ctx)
//            ctx.FOR() != null -> this.visitForStatement(ctx)
//            ctx.WHILE() != null -> {
//                if (ctx.DO() == null) this.visitWhileStatement(ctx)
//                else this.visitDoWhileStatement(ctx)
//            }
//
//            ctx.TRY() != null -> {
//                if (ctx.resourceSpecification() == null) this.visitTryStatement(ctx)
//                else this.visitTryWithResourcesStatement(ctx)
//            }
//
//            ctx.SWITCH() != null -> this.visitSwitchStatement(ctx)
//            ctx.SYNCHRONIZED() != null -> this.visitSynchronizedStatement(ctx)
//            ctx.RETURN() != null -> this.visitReturnStatement(ctx)
//            ctx.THROW() != null -> this.visitThrowStatement(ctx)
//            ctx.BREAK() != null -> this.visitBreakStatement()
//            ctx.CONTINUE() != null -> this.visitContinueStatement()
//            ctx.YIELD() != null -> this.visitYieldStatement(ctx)
//            ctx.statementExpression != null -> this.visitStatementExpression(ctx)
//            ctx.switchExpression() != null -> this.visitSwitchExpression(ctx.switchExpression()) as Statement?
//            ctx.identifierLabel != null -> this.visitLabeledStatement(ctx)
//            else -> this.createUnsupportedExpression(ctx)
//        } as Statement?
//
//        statement?.metadata = getSourceMetadata(ctx)
//        return statement
//    }
//
//    private fun visitAssertStatement(ctx: JavaParser.StatementContext): Statement {
//        return createUnsupportedExpression(ctx)
//    }
//
//    private fun visitIfStatement(ctx: JavaParser.StatementContext): IfStatement {
//        val ifStatement = IfStatement()
//        val ifBlock = IfBlock()
//
//        ifBlock.condition = this.visitParExpression(ctx.parExpression()) as Expression?
//        ifBlock.body = this.visitStatement(ctx.statement()?.first()) as Statement?
//        ifStatement.ifBlock = ifBlock
//
//        fun visitElseIf(ifStatement: IfStatement, ctx: JavaParser.StatementContext?): IfStatement {
//            val curBlock = this.visitStatement(ctx) as Statement?
//
//            if (curBlock is IfStatement) {
//                ifStatement.elseIf.add(curBlock.ifBlock)
//                if (ctx?.ELSE() != null) {
//                    visitElseIf(ifStatement, ctx.statement()?.last())
//                }
//            } else {
//                ifStatement.elseBlock = curBlock
//            }
//
//            return ifStatement
//        }
//
//        if (ctx.ELSE() != null) {
//            visitElseIf(ifStatement, ctx.statement().last())
//        }
//
//        return ifStatement
//    }
//
//    /**
//     * @return Expression
//     */
//    override fun visitParExpression(ctx: JavaParser.ParExpressionContext?): Any? {
//        return this.visitExpression(ctx?.expression()) as Expression?
//    }
//
//    private fun visitForStatement(ctx: JavaParser.StatementContext): ForStatement {
//        val forStatement = ForStatement()
//        forStatement.signature = this.visitForControl(ctx.forControl()) as ForSignature
//        forStatement.body = this.visitStatement(ctx.statement()?.firstOrNull()) as Statement
//        return forStatement
//    }
//
//    /**
//     * @return ForSignature
//     */
//    override fun visitForControl(ctx: JavaParser.ForControlContext?): Any {
//        return when {
//            ctx?.enhancedForControl() != null -> this.visitEnhancedForControl(ctx.enhancedForControl())
//            else -> {
//                val signature = BasicForSignature()
//                signature.initializer = this.visitForInit(ctx?.forInit()) as ForInit?
//                signature.expression = this.visitExpression(ctx?.expression()) as Expression?
//                signature.update = this.visitExpressionList(ctx?.forUpdate) as ExpressionList?
//                return signature
//            }
//        }
//    }
//
//    /**
//     * @return EnhancedForSignature
//     */
//    override fun visitEnhancedForControl(ctx: JavaParser.EnhancedForControlContext?): Any {
//        val signature = EnhancedForSignature()
//        signature.modifierList = visitModifierList(ctx?.variableModifier())
//
//        val variable = LocalVariableDeclaration()
//        variable.type = if (ctx?.VAR() != null) "var" else this.visitTypeType(ctx?.typeType()) as String
//        variable.fieldItem.add(FieldItem().also {
//            it.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId()) as String
//        })
//
//        signature.localVariableDeclaration = variable
//        signature.expression = this.visitExpression(ctx?.expression()) as Expression
//
//        return signature
//    }
//
//    private fun visitWhileStatement(ctx: JavaParser.StatementContext): WhileStatement {
//        val whileStatement = WhileStatement()
//        whileStatement.condition = this.visitParExpression(ctx.parExpression()) as Expression
//        whileStatement.body = this.visitStatement(ctx.statement().firstOrNull()) as Statement?
//        return whileStatement
//    }
//
//    private fun visitDoWhileStatement(ctx: JavaParser.StatementContext): DoStatement {
//        val doStatement = DoStatement()
//        doStatement.body = this.visitStatement(ctx.statement().firstOrNull()) as Statement?
//        doStatement.condition = this.visitParExpression(ctx.parExpression()) as Expression
//        return doStatement
//    }
//
//    private fun visitTryStatement(ctx: JavaParser.StatementContext): TryStatement {
//        val tryStatement = TryStatement()
//        tryStatement.tryBody = this.visitBlock(ctx.block()) as Statement?
//        tryStatement.catchClauseList = createCatchClauseList(ctx.catchClause())
//        tryStatement.finallyClause = this.visitFinallyBlock(ctx.finallyBlock()) as Statement?
//        return tryStatement
//    }
//
//    /**
//     * @return CatchClause
//     */
//    override fun visitCatchClause(ctx: JavaParser.CatchClauseContext?): Any {
//        val catchClause = CatchClause()
//        catchClause.modifierList = this.visitModifierList(ctx?.variableModifier())
//        catchClause.catchTypeList = this.visitCatchType(ctx?.catchType()) as ModuleNameList
//        catchClause.identifier = this.visitIdentifier(ctx?.identifier()) as String
//        catchClause.body = this.visitBlock(ctx?.block()) as Block?
//        return catchClause
//    }
//
//    /**
//     * @return ModuleNameList
//     */
//    override fun visitCatchType(ctx: JavaParser.CatchTypeContext?): Any {
//        val list = ModuleNameList()
//
//        ctx?.qualifiedName()
//            ?.mapNotNull { this.visitQualifiedName(it) }
//            ?.filterIsInstance<String>()
//            ?.forEach { list.names.add(it) }
//
//        return list
//    }
//
//    /**
//     * @return Block?
//     */
//    override fun visitFinallyBlock(ctx: JavaParser.FinallyBlockContext?): Any? {
//        return this.visitBlock(ctx?.block())
//    }
//
//    private fun visitTryWithResourcesStatement(ctx: JavaParser.StatementContext): TryStatement {
//        val tryStatement = TryStatement()
//        tryStatement.tryBody = this.visitBlock(ctx.block()) as Block
//        tryStatement.catchClauseList = createCatchClauseList(ctx.catchClause())
//        tryStatement.finallyClause = this.visitFinallyBlock(ctx.finallyBlock()) as Statement?
//
//        (tryStatement.tryBody as Block).statements.addAll(
//            0,
//            (this.visitResourceSpecification(ctx.resourceSpecification()) as List<*>).filterIsInstance<LocalVariableDeclaration>()
//        )
//
//        return tryStatement
//    }
//
//    private fun createCatchClauseList(ctx: MutableList<JavaParser.CatchClauseContext>?): CatchClauseList {
//        return CatchClauseList().also {
//            ctx?.map { this.visitCatchClause(it) }
//                ?.filterIsInstance<CatchClause>()
//                ?.forEach { c -> it.clauses.add(c) }
//        }
//    }
//
//    /**
//     * @return List<LocalVariableDeclaration>
//     */
//    override fun visitResourceSpecification(ctx: JavaParser.ResourceSpecificationContext?): Any? {
//        return this.visitResources(ctx?.resources())
//    }
//
//    /**
//     * @return List<LocalVariableDeclaration>
//     */
//    override fun visitResources(ctx: JavaParser.ResourcesContext?): Any? {
//        return ctx?.resource()?.mapNotNull(this::visitResource)?.filterIsInstance<LocalVariableDeclaration>()
//    }
//
//    /**
//     * @return LocalVariableDeclaration
//     */
//    override fun visitResource(ctx: JavaParser.ResourceContext?): Any {
//        val variable = LocalVariableDeclaration()
//        variable.type = this.visitClassOrInterfaceType(ctx?.classOrInterfaceType()) as? String ?: "var"
//
//        val item = FieldItem()
//        item.modifierList = this.visitModifierList(ctx?.variableModifier())
//        item.identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId()) as? String
//            ?: this.visitIdentifier(ctx?.identifier()) as? String
//        item.variableInitializer = this.visitExpression(ctx?.expression()) as? Expression
//
//        variable.fieldItem.add(item)
//
//        return variable
//    }
//
//    private fun visitSwitchStatement(ctx: JavaParser.StatementContext): Statement {
//        val switch = SwitchStatement()
//        switch.subject = this.visitParExpression(ctx.parExpression()) as Expression
//
//        val body = SwitchBlock()
//
//        ctx.switchBlockStatementGroup()
//            .map { this.visitSwitchBlockStatementGroup(it) }
//            .filterIsInstance<List<SwitchRule>>()
//            .forEach { body.switchRule.addAll(it) }
//
//        switch.switchBlock = body
//
//        return switch
//    }
//
//    override fun visitSwitchBlockStatementGroup(ctx: JavaParser.SwitchBlockStatementGroupContext?): Any {
//        val rules = mutableListOf<SwitchRule>()
//
//        ctx?.switchLabel()?.forEachIndexed { i, l ->
//            rules.add(SwitchRule().also {
//                it.pattern = this.visitSwitchLabel(l) as SwitchPattern
//                it.body = this.visitBlockStatement(ctx.blockStatement(i)) as Statement?
//            })
//        }
//
//        return rules
//    }
//
//    override fun visitSwitchLabel(ctx: JavaParser.SwitchLabelContext?): Any {
//        return SwitchPattern().also {
//            it.isDefault = ctx?.DEFAULT() != null
//            when {
//                ctx?.constantExpression != null -> it.valuePattern =
//                    this.visitExpression(ctx.constantExpression) as Expression
//
//                ctx?.enumConstantName != null -> it.valuePattern =
//                    LiteralExpression().also { l -> l.literal = ctx.enumConstantName.text }
//
//                ctx?.varName != null -> it.varPattern = LocalVariableDeclaration().also { lvd ->
//                    lvd.type = this.visitTypeType(ctx.typeType()) as String
//                    lvd.fieldItem.add(FieldItem().also { i ->
//                        i.identifier = this.visitIdentifier(ctx.identifier()) as String
//                    })
//                }
//            }
//        }
//    }
//
//    override fun visitSwitchExpression(ctx: JavaParser.SwitchExpressionContext?): Any {
//        val expression = SwitchExpression()
//
//        val switch = SwitchStatement()
//        switch.subject = this.visitParExpression(ctx?.parExpression()) as Expression
//
//        val body = SwitchBlock()
//        ctx?.switchLabeledRule()
//            ?.map { this.visitSwitchLabeledRule(it) }
//            ?.filterIsInstance<SwitchRule>()
//            ?.forEach { body.switchRule.add(it) }
//
//        switch.switchBlock = body
//        switch.metadata = getSourceMetadata(ctx!!)
//
//        expression.expression = switch
//
//        return expression
//    }
//
//    override fun visitSwitchLabeledRule(ctx: JavaParser.SwitchLabeledRuleContext?): Any {
//        val pattern = SwitchPattern().also {
//            it.isDefault = ctx?.DEFAULT() != null
//            when {
//                ctx?.expressionList() != null -> it.multiValuePattern =
//                    this.visitExpressionList(ctx.expressionList()) as ExpressionList?
//
//                ctx?.NULL_LITERAL() != null -> it.valuePattern = LiteralExpression().also { l->
//                    l.literal = "null"
//                    l.metadata = getSourceMetadata(ctx)
//                }
//                ctx?.guardedPattern() != null -> it.guardedPattern =
//                    this.visitGuardedPattern(ctx.guardedPattern()) as GuardedPattern
//            }
//        }
//
//        return SwitchRule().also {
//            it.pattern = pattern
//            it.body = this.visitSwitchRuleOutcome(ctx?.switchRuleOutcome()) as Statement?
//        }
//    }
//
//    override fun visitGuardedPattern(ctx: JavaParser.GuardedPatternContext?): Any {
//        var pattern = GuardedPattern()
//
//        if (ctx?.guardedPattern() != null) {
//            pattern = this.visitGuardedPattern(ctx.guardedPattern()) as GuardedPattern
//        }
//
//        if (ctx?.typeType() != null) {
//            pattern.localVariableDeclaration = LocalVariableDeclaration().also { l ->
//                l.type = this.visitTypeType(ctx.typeType()) as String?
//                l.fieldItem.add(FieldItem().also { f ->
//                    f.modifierList = this.visitModifierList(ctx.variableModifier())
//                    f.identifier = this.visitIdentifier(ctx.identifier()) as String
//                })
//            }
//        }
//
//        if (pattern.andExpressionList == null && ctx?.expression() != null) {
//            pattern.andExpressionList = ExpressionList()
//        }
//
//        ctx?.expression()
//            ?.mapNotNull { e -> this.visitExpression(e) as Expression? }
//            ?.forEach { e -> pattern.andExpressionList.expressions.add(e) }
//
//        return pattern
//    }
//
//    private fun visitSynchronizedStatement(ctx: JavaParser.StatementContext): Statement {
//        val block = Block()
//
//        block.statements.add(this.visitParExpression(ctx.parExpression()) as Statement)
//        block.statements.addAll((this.visitBlock(ctx.block()) as Block).statements)
//
//        return block
//    }
//
//    private fun visitReturnStatement(ctx: JavaParser.StatementContext): Statement {
//        val returnStatement = ReturnStatement()
//        returnStatement.returnValue = this.visitExpression(ctx.expression().firstOrNull()) as Expression?
//        return returnStatement
//    }
//
//    private fun visitThrowStatement(ctx: JavaParser.StatementContext): Statement {
//        val throwStatement = ThrowStatement()
//        throwStatement.throws = this.visitExpression(ctx.expression().first()) as Expression
//        return throwStatement
//    }
//
//    private fun visitBreakStatement(): Statement {
//        return BreakStatement()
//    }
//
//    private fun visitContinueStatement(): Statement {
//        return ContinueStatement()
//    }
//
//    private fun visitYieldStatement(ctx: JavaParser.StatementContext): Statement {
//        return visitExpression(ctx.expression().first()) as Expression
//    }
//
//    private fun visitStatementExpression(ctx: JavaParser.StatementContext): Statement {
//        return this.visitExpression(ctx.statementExpression) as Expression
//    }
//
//    private fun visitLabeledStatement(ctx: JavaParser.StatementContext): Statement {
//        return visitStatement(ctx.statement().first()) as Statement
//    }
//
//    /**
//     * @return Expression subclass
//     */
//    override fun visitExpression(ctx: JavaParser.ExpressionContext?): Any? {
//        if (ctx == null) return null
//
//        // TODO Test extensively. This pattern matching clause could be prone to wrong identification.
//
//        val expression = when {
//            ctx.primary() != null -> this.visitPrimary(ctx.primary())
//            ctx.bop != null && ctx.bop.equals(".") -> this.visitCallExpression(ctx)
//            ctx.expression().size == 2 && ctx.text.contains("[") -> this.visitArrayAccessExpression(ctx)
//            ctx.methodCall() != null -> this.visitMethodCall(ctx.methodCall())
//            ctx.creator() != null -> this.visitCreator(ctx.creator())
//            // TODO Add: '(' annotation* typeType ('&' typeType)* ')' expression
//            ctx.postfix != null || ctx.prefix != null -> this.visitUnaryExpression(ctx)
//            ctx.expression().size == 2
//                    && (ctx.bop != null || (ctx.text.contains("<") || ctx.text.contains(">")))
//            -> this.visitBinaryExpression(ctx)
//
//            ctx.INSTANCEOF() != null -> this.visitTypeCheck(ctx)
//            ctx.expression().size == 3 && ctx.bop.text == "?" -> this.visitConditionalExpression(ctx)
//            ctx.lambdaExpression() != null -> this.visitLambdaExpression(ctx.lambdaExpression())
//            ctx.switchExpression() != null -> this.visitSwitchExpression(ctx.switchExpression())
//            ctx.expression()?.size == 1 && ctx.identifier() != null -> this.visitMethodReference(ctx)
//            ctx.typeType() != null || ctx.classType() != null -> this.visitMethodReference(ctx)
//            else -> createUnsupportedExpression(ctx)
//        } as? Expression
//
//        expression?.metadata = getSourceMetadata(ctx)
//        return expression
//    }
//
//    override fun visitPrimary(ctx: JavaParser.PrimaryContext?): Any? {
//        fun createLiteral(name: String): LiteralExpression {
//            return LiteralExpression()
//                .also { it.literal = name }
//        }
//
//        return when {
//            ctx == null -> null
//            ctx.expression() != null -> this.visitExpression(ctx.expression())
//            ctx.THIS() != null -> createLiteral("this")
//            ctx.SUPER() != null -> createLiteral("super")
//            ctx.literal() != null -> this.visitLiteral(ctx.literal())
//            ctx.identifier() != null -> createLiteral(this.visitIdentifier(ctx.identifier()) as String)
//            ctx.typeTypeOrVoid() != null -> createLiteral(this.visitTypeTypeOrVoid(ctx.typeTypeOrVoid()) as String + ".class")
//            // TODO Add: nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
//            else -> this.createUnsupportedExpression(ctx)
//        }
//    }
//
//    override fun visitLiteral(ctx: JavaParser.LiteralContext?): Any {
//        return LiteralExpression().also { it.literal = ctx?.text }
//    }
//
//    private fun visitCallExpression(ctx: JavaParser.ExpressionContext): Expression {
//        // TODO Add: expression chain / expression group
//        return createUnsupportedExpression(ctx)
//    }
//
//    private fun visitArrayAccessExpression(ctx: JavaParser.ExpressionContext): Expression {
//        return createUnsupportedExpression(ctx)
//    }
//
//    private fun visitTypeCheck(ctx: JavaParser.ExpressionContext): Expression {
//        val typeCheck = TypeCheckExpression()
//        typeCheck.subject = this.visitExpression(ctx.expression().first()) as Expression?
//        typeCheck.onType = TypeCheckOption().also {
//            ctx.typeType()?.firstOrNull()?.let { t -> it.type = this.visitTypeType(t) as String }
//            ctx.pattern()?.let { p -> it.pattern = this.visitPattern(p) as TypePattern }
//        }
//
//        return typeCheck
//    }
//
//    /**
//     * @return TypePattern
//     */
//    override fun visitPattern(ctx: JavaParser.PatternContext?): Any {
//        val pattern = TypePattern()
//        pattern.modifierList = visitModifierList(ctx?.variableModifier())
//        pattern.type = this.visitTypeType(ctx?.typeType()) as String?
//        pattern.identifier = this.visitIdentifier(ctx?.identifier()) as String?
//        return pattern
//    }
//
//    private fun visitUnaryExpression(ctx: JavaParser.ExpressionContext): Expression {
//        val expr = UnaryExpression()
//        expr.expression = this.visitExpression(ctx.expression().first()) as Expression?
//        return expr
//    }
//
//    private fun visitBinaryExpression(ctx: JavaParser.ExpressionContext): Expression {
//        val expr = BinaryExpression()
//        expr.operator = when (ctx.bop.text) {
//            "=" -> BinaryOperator.ASSIGN
//            "+=", "-=", "*=", "/=", "&=", "|=", "^=", ">>=", ">>>=", "<<=", "%=" -> BinaryOperator.OPERATOR_ASSIGN
//            else -> BinaryOperator.ARITHMETIC
//        }
//
//        expr.leftSide = this.visitExpression(ctx.expression().first()) as Expression
//        expr.rightSide = this.visitExpression(ctx.expression().last()) as Expression
//
//        return expr
//    }
//
//    private fun visitConditionalExpression(ctx: JavaParser.ExpressionContext): Expression {
//        val expr = ConditionalExpression()
//
//        expr.condition = this.visitExpression(ctx.expression(0)) as Expression
//        expr.onTrue = this.visitExpression(ctx.expression(1)) as Expression
//        expr.onFalse = this.visitExpression(ctx.expression(2)) as Expression
//
//        return expr
//    }
//
//    override fun visitLambdaExpression(ctx: JavaParser.LambdaExpressionContext?): Any {
//        val lambda = LambdaExpression()
//        lambda.parameterList = this.visitLambdaParameters(ctx?.lambdaParameters()) as ParameterList?
//        lambda.body = this.visitLambdaBody(ctx?.lambdaBody()) as LambdaBody?
//        return lambda
//    }
//
//    override fun visitLambdaParameters(ctx: JavaParser.LambdaParametersContext?): Any? {
//        val params = ParameterList()
//
//        ctx?.identifier()
//            ?.mapNotNull { this.visitIdentifier(it) as String? }
//            ?.forEach {
//                val param = Parameter()
//                param.identifier = it
//                param.type = "" // TODO need a type?
//            }
//
//        (this.visitFormalParameterList(ctx?.formalParameterList()) as ParameterList?)
//            ?.let { params.parameters.addAll(it.parameters) }
//
//        (this.visitLambdaLVTIList(ctx?.lambdaLVTIList()) as ParameterList?)
//            ?.let { params.parameters.addAll(it.parameters) }
//
//        return params.value()
//    }
//
//    /**
//     * @return ParameterList
//     */
//    override fun visitLambdaLVTIList(ctx: JavaParser.LambdaLVTIListContext?): Any? {
//        val params = ParameterList()
//
//        ctx?.lambdaLVTIParameter()
//            ?.mapNotNull { this.visitLambdaLVTIParameter(it) as Parameter? }
//            ?.forEach { params.parameters.add(it) }
//
//        return params.value()
//    }
//
//    /**
//     * @return Parameter
//     */
//    override fun visitLambdaLVTIParameter(ctx: JavaParser.LambdaLVTIParameterContext?): Any {
//        val param = Parameter()
//        param.modifierList = this.visitModifierList(ctx?.variableModifier())
//        param.type = "var"
//        param.identifier = this.visitIdentifier(ctx?.identifier()) as String
//
//        return param
//    }
//
//    override fun visitLambdaBody(ctx: JavaParser.LambdaBodyContext?): Any {
//        val lambdaBody = LambdaBody()
//        lambdaBody.expression = this.visitExpression(ctx?.expression()) as Expression?
//        lambdaBody.body = this.visitBlock(ctx?.block()) as Statement?
//
//        return lambdaBody
//    }
//
//    private fun visitMethodReference(ctx: JavaParser.ExpressionContext): Expression {
//        // TODO Implement method references: expression::, typeType::, classType::
//        return createUnsupportedExpression(ctx)
//    }
//
//    override fun visitMethodCall(ctx: JavaParser.MethodCallContext?): Any {
//        val call = FunctionCallExpression()
//        call.identifier = when {
//            ctx?.identifier() != null -> this.visitIdentifier(ctx.identifier()) as String
//            ctx?.THIS() != null -> "this"
//            ctx?.SUPER() != null -> "super"
//            else -> null
//        }
//
//        call.argumentList = this.visitExpressionList(ctx?.expressionList()) as ExpressionList?
//
//        return call
//    }
//
//    /**
//     * @return ExpressionList
//     */
//    override fun visitExpressionList(ctx: JavaParser.ExpressionListContext?): Any? {
//        val list = ExpressionList()
//        ctx?.expression()
//            ?.mapNotNull { this.visitExpression(it) }
//            ?.filterIsInstance<Expression>()
//            ?.forEach { list.expressions.add(it) }
//
//        return list.value()
//    }
//
//    override fun visitCreator(ctx: JavaParser.CreatorContext?): Any {
//        // TODO Implement creator element that can handle initializing of arrays of classes
//        return createUnsupportedExpression(ctx)
//    }
//
//    /**
//     * START OF DEBUGGING FUNCTION OVERRIDES
//     */
//    override fun visit(tree: ParseTree?): Any? {
//        Log.d("Visiting ${tree?.javaClass?.simpleName}")
//        return super.visit(tree)
//    }
//
//    override fun visitChildren(node: RuleNode?): Any? {
//        Log.d("${"\t".repeat(node?.getDepth() ?: 0)}${node?.javaClass?.simpleName}")
//        return super.visitChildren(node)
//    }
//
//    override fun visitTerminal(node: TerminalNode?): Any? {
//        Log.d("${"\t".repeat(node?.getDepth() ?: 0)}${node?.javaClass?.simpleName}")
//        return super.visitTerminal(node)
//    }
//
//    /**
//     * END OF DEBUGGING FUNCTION OVERRIDES
//     */
//
    fun ModifierList.value(): ModifierList? = if (this.modifiers.size > 0) this else null
//    fun ParameterList.value(): ParameterList? = if (this.parameters.size > 0) this else null
//    fun ModuleBody.value(): ModuleBody? = if (this.members.size > 0) this else null
//    fun Block.value(): Block? = if (this.statements.size > 0) this else null
//    fun ExpressionList.value(): ExpressionList? = if (this.expressions.size > 0) this else null
}

