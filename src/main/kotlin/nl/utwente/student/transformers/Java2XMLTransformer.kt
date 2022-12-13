package nl.utwente.student.transformers

import nl.utwente.student.model.JavaFile
import nl.utwente.student.visitor.java.JavaParser
import nl.utwente.student.visitor.java.JavaParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Java2XMLTransformer(val file: File): JavaParserBaseVisitor<List<Node?>?>() {
    private lateinit var currentDoc: Document
    private var moduleName = "?"
    private var packageName = "?"
    val documents = mutableListOf<JavaFile>()

    private fun getModuleMetadata(ctx: JavaParser.TypeDeclarationContext): Element {
        val metadata = currentDoc.createElement("Metadata")

        val fileElement = currentDoc.createElement("File")
        fileElement.setAttribute("path", file.absolutePath)
        fileElement.setAttribute("name", file.name)

        metadata.appendChild(fileElement)
        metadata.appendChild(getSourceMetadata(ctx, "Location"))
        return metadata
    }

    /**
     * Metadata about the source code that needs to be added to the module, module members, statements and expressions.
     */
    private fun getSourceMetadata(ctx: ParserRuleContext, elementName: String = "Metadata"): Element {
        val sourceMetadata = currentDoc.createElement(elementName)

        sourceMetadata.setAttribute("startLine", ctx.start.line.toString())
        sourceMetadata.setAttribute("endLine", ctx.stop.line.toString())
        sourceMetadata.setAttribute("startOffset", ctx.start.charPositionInLine.toString())
        sourceMetadata.setAttribute("endOffset", ctx.stop.charPositionInLine.toString())

        return sourceMetadata
    }

    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext?): List<Node?>? {
        return super.visitCompilationUnit(ctx)
    }

    override fun visitPackageDeclaration(ctx: JavaParser.PackageDeclarationContext?): List<Node?>? {
        packageName = ctx?.qualifiedName()?.identifier()?.joinToString(".") { it.text }.toString()
        return null // Prevent these contents from being visited.
    }

    override fun visitImportDeclaration(ctx: JavaParser.ImportDeclarationContext?): List<Node?>? {
        return null // Prevent these contents from being visited.
    }

    override fun visitTypeDeclaration(ctx: JavaParser.TypeDeclarationContext?): List<Node?>? {
        print("Visiting '${file.name}', lines=[${ctx!!.start.line}:${ctx.stop.line}]")

        val factory = DocumentBuilderFactory.newInstance()
        val docBuilder = factory.newDocumentBuilder()

        currentDoc = docBuilder.newDocument()
        val module = currentDoc.createElement("Module")
        module.setAttribute("xmlns", "https://www.utwente.nl")
        module.appendChild(this.getModuleMetadata(ctx))

        currentDoc.appendChild(module)

        /*
        Create ModuleHeader and its contents
         */
        val moduleHeader = currentDoc.createElement("ModuleHeader")
        module.appendChild(moduleHeader)

        val moduleModifierList = currentDoc.createElement("ModifierList")
        moduleHeader.appendChild(moduleModifierList)

        ctx.classOrInterfaceModifier()?.forEach {
            val modifierItem = this.visitClassOrInterfaceModifier(it)
            moduleModifierList.appendChild(modifierItem)
        }

        /*
        Create ModuleBody and its contents, depending on which declaration isn't null.
         */
        ctx.classDeclaration()?.let { module.appendChild(this.visitClassDeclaration(it))}
        ctx.enumDeclaration()?.let { module.appendChild(this.visitEnumDeclaration(it))}
        ctx.interfaceDeclaration()?.let { module.appendChild(this.visitInterfaceDeclaration(it))}
        ctx.recordDeclaration()?.let { module.appendChild(this.visitRecordDeclaration(it))}

        println(". Done!")
        return module.returnValue()
    }

    override fun visitClassOrInterfaceModifier(ctx: JavaParser.ClassOrInterfaceModifierContext?): List<Node?>? {
        val item = currentDoc.createElement("Item")
        var value = ""

        ctx?.annotation()?.let {
            this.visitAnnotation(it)
        }

        ctx?.PUBLIC()?.let {value = "public"}
        ctx?.PROTECTED()?.let {value = "protected"}
        ctx?.PRIVATE()?.let {value = "private"}
        ctx?.STATIC()?.let {value = "static"}
        ctx?.ABSTRACT()?.let {value = "abstract"}
        ctx?.FINAL()?.let {value = "final"}
        ctx?.SEALED()?.let {value = "sealed"}
        ctx?.NON_SEALED()?.let {value = "open"}

        item.appendChild(currentDoc.createTextNode(value))

        return item.returnValue()
    }

    override fun visitVariableModifier(ctx: JavaParser.VariableModifierContext?): List<Node?>? {
        return super.visitVariableModifier(ctx)
    }

    override fun visitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?): List<Node?>? {
        // TODO(Handle Inner Classes, creating new module header for them etc.)
        val moduleHeader = currentDoc.getElementsByTagName("ModuleHeader").item(0) as Element
        moduleName = ctx?.identifier()?.text.toString()
        moduleHeader.setAttribute("identifier", moduleName)
        moduleHeader.setAttribute("moduleType", "class")

        if (ctx?.EXTENDS() != null) {
            val extends = currentDoc.createElement("Extends")
            extends.appendChild(visitTypeType(ctx.typeType()))

            moduleHeader.appendChild(extends)
        }

        if (ctx?.IMPLEMENTS() != null) {
            val implementsList = currentDoc.createElement("Implements")
            ctx.typeList()?.get(0)?.typeType()?.forEach {
                val name = currentDoc.createElement("Name")
                name.appendChild(this.visitTypeType(it))

                implementsList.appendChild(name)
            }

            moduleHeader.appendChild(implementsList)
        }

        return this.visitClassBody(ctx?.classBody())
    }

    override fun visitClassBody(ctx: JavaParser.ClassBodyContext?): List<Node?>? {
        val moduleBody = currentDoc.createElement("ModuleBody")
        ctx?.classBodyDeclaration()?.forEach {
            visitClassBodyDeclaration(it)?.let { classBody -> moduleBody.appendChild(classBody) }
        }
        return moduleBody.returnValue()
    }

    override fun visitClassBodyDeclaration(ctx: JavaParser.ClassBodyDeclarationContext?): List<Node?>? {
        val metadata = getSourceMetadata(ctx!!)

        if(ctx.block() != null) {
            val isStatic = ctx.STATIC() != null

            val initializer = currentDoc.createElement("Initializer")
            initializer.appendChild(metadata)
            initializer.setAttribute("isStatic", isStatic.toString())

            val body = visitBlock(ctx.block())
            body?.let { initializer?.appendChild(it) }

            return initializer.returnValue()
        }

        if (ctx.memberDeclaration() != null) {
            val modifierList = currentDoc.createElement("ModifierList")
            ctx.modifier()?.map { this.visitModifier(it) }?.forEach { modifierList.appendChild(it) }

            val members = this.visitMemberDeclaration(ctx.memberDeclaration())
            members?.forEach {
                it?.appendChild(metadata)
                it?.appendChild(modifierList)
            }

            return members
        }

        return null
    }

    override fun visitTypeParameters(ctx: JavaParser.TypeParametersContext?): List<Node?>? {
        return super.visitTypeParameters(ctx)
    }

    override fun visitTypeParameter(ctx: JavaParser.TypeParameterContext?): List<Node?>? {
        return super.visitTypeParameter(ctx)
    }

    override fun visitTypeBound(ctx: JavaParser.TypeBoundContext?): List<Node?>? {
        return super.visitTypeBound(ctx)
    }

    override fun visitEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?): List<Node?>? {
        return super.visitEnumDeclaration(ctx)
    }

    override fun visitEnumConstants(ctx: JavaParser.EnumConstantsContext?): List<Node?>? {
        return super.visitEnumConstants(ctx)
    }

    override fun visitEnumConstant(ctx: JavaParser.EnumConstantContext?): List<Node?>? {
        return super.visitEnumConstant(ctx)
    }

    override fun visitEnumBodyDeclarations(ctx: JavaParser.EnumBodyDeclarationsContext?): List<Node?>? {
        return super.visitEnumBodyDeclarations(ctx)
    }

    override fun visitInterfaceDeclaration(ctx: JavaParser.InterfaceDeclarationContext?): List<Node?>? {
        return super.visitInterfaceDeclaration(ctx)
    }

    override fun visitInterfaceBody(ctx: JavaParser.InterfaceBodyContext?): List<Node?>? {
        return super.visitInterfaceBody(ctx)
    }

    override fun visitMemberDeclaration(ctx: JavaParser.MemberDeclarationContext?): List<Node?>? {
        return super.visitMemberDeclaration(ctx)
    }

    override fun visitMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?): List<Node?>? {
        val unit = currentDoc.createElement("Unit")

        val type: Text = this.visitTypeTypeOrVoid(ctx?.typeTypeOrVoid())?.first() as Text
        unit.setAttribute("type", type.wholeText)

        val identifier: Text = this.visitIdentifier(ctx?.identifier())?.first() as Text
        unit.setAttribute("name", identifier.wholeText)

        val parameterList: Element = this.visitFormalParameters(ctx?.formalParameters())?.first() as Element
        unit.appendChild(parameterList)

        val body: Element? = this.visitMethodBody(ctx?.methodBody())?.first() as Element?
        unit.appendChild(body)

        return unit.returnValue()
    }

    override fun visitMethodBody(ctx: JavaParser.MethodBodyContext?): List<Node?>? {
        return currentDoc.createElement("Body").returnValue()
    }

    override fun visitTypeTypeOrVoid(ctx: JavaParser.TypeTypeOrVoidContext?): List<Node?>? {
        return if (ctx?.VOID() != null) {
            currentDoc.createTextNode("void").returnValue()
        } else {
            this.visitTypeType(ctx?.typeType())
        }
    }

    override fun visitGenericMethodDeclaration(ctx: JavaParser.GenericMethodDeclarationContext?): List<Node?>? {
        return super.visitGenericMethodDeclaration(ctx)
    }

    override fun visitGenericConstructorDeclaration(ctx: JavaParser.GenericConstructorDeclarationContext?): List<Node?>? {
        return super.visitGenericConstructorDeclaration(ctx)
    }

    override fun visitConstructorDeclaration(ctx: JavaParser.ConstructorDeclarationContext?): List<Node?>? {
        val constructor = currentDoc.createElement("Constructor")

        val parameterList = this.visitFormalParameters(ctx?.formalParameters())
        constructor.appendChild(parameterList?.first())

        val body = this.visitBlock(ctx?.constructorBody)
        constructor.appendChild(body?.first())

        return constructor.returnValue()
    }

    override fun visitFieldDeclaration(ctx: JavaParser.FieldDeclarationContext?): List<Node?>? {
        return null
    }

    fun visitFieldVariableDeclaration(ctx: JavaParser.FieldDeclarationContext?): List<Element>? {
        val type = this.visitTypeType(ctx?.typeType())?.first() as Text

        return ctx?.variableDeclarators()?.variableDeclarator()?.map {
            val field = this.visitVariableDeclarator(it)?.first() as Element
            field.setAttribute("type", type.wholeText)
            field
        }
    }

    override fun visitInterfaceBodyDeclaration(ctx: JavaParser.InterfaceBodyDeclarationContext?): List<Node?>? {
        return super.visitInterfaceBodyDeclaration(ctx)
    }

    override fun visitInterfaceMemberDeclaration(ctx: JavaParser.InterfaceMemberDeclarationContext?): List<Node?>? {
        return super.visitInterfaceMemberDeclaration(ctx)
    }

    override fun visitConstDeclaration(ctx: JavaParser.ConstDeclarationContext?): List<Node?>? {
        return super.visitConstDeclaration(ctx)
    }

    override fun visitConstantDeclarator(ctx: JavaParser.ConstantDeclaratorContext?): List<Node?>? {
        return super.visitConstantDeclarator(ctx)
    }

    override fun visitInterfaceMethodDeclaration(ctx: JavaParser.InterfaceMethodDeclarationContext?): List<Node?>? {
        return super.visitInterfaceMethodDeclaration(ctx)
    }

    override fun visitInterfaceMethodModifier(ctx: JavaParser.InterfaceMethodModifierContext?): List<Node?>? {
        return super.visitInterfaceMethodModifier(ctx)
    }

    override fun visitGenericInterfaceMethodDeclaration(ctx: JavaParser.GenericInterfaceMethodDeclarationContext?): List<Node?>? {
        return super.visitGenericInterfaceMethodDeclaration(ctx)
    }

    override fun visitInterfaceCommonBodyDeclaration(ctx: JavaParser.InterfaceCommonBodyDeclarationContext?): List<Node?>? {
        return super.visitInterfaceCommonBodyDeclaration(ctx)
    }

    override fun visitVariableDeclarators(ctx: JavaParser.VariableDeclaratorsContext?): List<Node?>? {
        return super.visitVariableDeclarators(ctx)
    }

    override fun visitVariableDeclarator(ctx: JavaParser.VariableDeclaratorContext?): List<Node?>? {
        val field = currentDoc.createElement("Field")
        val identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())?.first() as Text
        field.setAttribute("name", identifier.wholeText)

        val body = currentDoc.createElement("Body")
//        body.appendChild(this.visitVariableInitializer(ctx?.variableInitializer())) TODO implement

        return field.returnValue()
    }

    override fun visitVariableDeclaratorId(ctx: JavaParser.VariableDeclaratorIdContext?): List<Node?>? {
        return currentDoc.createTextNode(ctx?.text).returnValue()
    }

    override fun visitArrayInitializer(ctx: JavaParser.ArrayInitializerContext?): List<Node?>? {
        return null // TODO implement array initializer
    }

    override fun visitClassOrInterfaceType(ctx: JavaParser.ClassOrInterfaceTypeContext?): List<Node?>? {
        var typeName = ""

        ctx?.identifier()?.forEach { typeName += (visitIdentifier(it)?.first() as Text).wholeText + "." }
        ctx?.typeIdentifier()?.let { typeName += (visitTypeIdentifier(it)?.first() as Text).wholeText }

        return currentDoc.createTextNode(typeName).returnValue()
    }

    override fun visitTypeArgument(ctx: JavaParser.TypeArgumentContext?): List<Node?>? {
        return this.visitTypeType(ctx?.typeType())
    }

    override fun visitQualifiedNameList(ctx: JavaParser.QualifiedNameListContext?): List<Node?>? {
        return super.visitQualifiedNameList(ctx)
    }

    override fun visitFormalParameters(ctx: JavaParser.FormalParametersContext?): List<Node?>? {
        return this.visitFormalParameterList(ctx?.formalParameterList())
    }

    override fun visitReceiverParameter(ctx: JavaParser.ReceiverParameterContext?): List<Node?>? {
        return super.visitReceiverParameter(ctx)
    }

    override fun visitFormalParameterList(ctx: JavaParser.FormalParameterListContext?): List<Node?>? {
        val parameterList = currentDoc.createElement("ParameterList")
        ctx?.formalParameter()?.forEach { parameterList.appendChild(this.visitFormalParameter(it)) }
        return parameterList.returnValue()
    }

    override fun visitFormalParameter(ctx: JavaParser.FormalParameterContext?): List<Node?>? {
        val parameter = currentDoc.createElement("Parameter")

        val type = this.visitTypeType(ctx?.typeType())?.first() as Text
        val identifier = this.visitVariableDeclaratorId(ctx?.variableDeclaratorId())?.first() as Text

        parameter.setAttribute("type", type.wholeText)
        parameter.setAttribute("identifier", identifier.wholeText)

        return parameter.returnValue()
    }

    override fun visitLastFormalParameter(ctx: JavaParser.LastFormalParameterContext?): List<Node?>? {
        return super.visitLastFormalParameter(ctx)
    }

    override fun visitLambdaLVTIList(ctx: JavaParser.LambdaLVTIListContext?): List<Node?>? {
        return super.visitLambdaLVTIList(ctx)
    }

    override fun visitLambdaLVTIParameter(ctx: JavaParser.LambdaLVTIParameterContext?): List<Node?>? {
        return super.visitLambdaLVTIParameter(ctx)
    }

    override fun visitQualifiedName(ctx: JavaParser.QualifiedNameContext?): List<Node?>? {
        return super.visitQualifiedName(ctx)
    }

    override fun visitLiteral(ctx: JavaParser.LiteralContext?): List<Node?>? {
        return super.visitLiteral(ctx)
    }

    override fun visitIntegerLiteral(ctx: JavaParser.IntegerLiteralContext?): List<Node?>? {
        return super.visitIntegerLiteral(ctx)
    }

    override fun visitFloatLiteral(ctx: JavaParser.FloatLiteralContext?): List<Node?>? {
        return super.visitFloatLiteral(ctx)
    }

    override fun visitAltAnnotationQualifiedName(ctx: JavaParser.AltAnnotationQualifiedNameContext?): List<Node?>? {
        return super.visitAltAnnotationQualifiedName(ctx)
    }

    override fun visitAnnotation(ctx: JavaParser.AnnotationContext?): List<Node?>? {
        return super.visitAnnotation(ctx)
    }

    override fun visitElementValuePairs(ctx: JavaParser.ElementValuePairsContext?): List<Node?>? {
        return super.visitElementValuePairs(ctx)
    }

    override fun visitElementValuePair(ctx: JavaParser.ElementValuePairContext?): List<Node?>? {
        return super.visitElementValuePair(ctx)
    }

    override fun visitElementValue(ctx: JavaParser.ElementValueContext?): List<Node?>? {
        return super.visitElementValue(ctx)
    }

    override fun visitElementValueArrayInitializer(ctx: JavaParser.ElementValueArrayInitializerContext?): List<Node?>? {
        return super.visitElementValueArrayInitializer(ctx)
    }

    override fun visitAnnotationTypeDeclaration(ctx: JavaParser.AnnotationTypeDeclarationContext?): List<Node?>? {
        return super.visitAnnotationTypeDeclaration(ctx)
    }

    override fun visitAnnotationTypeBody(ctx: JavaParser.AnnotationTypeBodyContext?): List<Node?>? {
        return super.visitAnnotationTypeBody(ctx)
    }

    override fun visitAnnotationTypeElementDeclaration(ctx: JavaParser.AnnotationTypeElementDeclarationContext?): List<Node?>? {
        return super.visitAnnotationTypeElementDeclaration(ctx)
    }

    override fun visitAnnotationTypeElementRest(ctx: JavaParser.AnnotationTypeElementRestContext?): List<Node?>? {
        return super.visitAnnotationTypeElementRest(ctx)
    }

    override fun visitAnnotationMethodOrConstantRest(ctx: JavaParser.AnnotationMethodOrConstantRestContext?): List<Node?>? {
        return super.visitAnnotationMethodOrConstantRest(ctx)
    }

    override fun visitAnnotationMethodRest(ctx: JavaParser.AnnotationMethodRestContext?): List<Node?>? {
        return super.visitAnnotationMethodRest(ctx)
    }

    override fun visitAnnotationConstantRest(ctx: JavaParser.AnnotationConstantRestContext?): List<Node?>? {
        return super.visitAnnotationConstantRest(ctx)
    }

    override fun visitDefaultValue(ctx: JavaParser.DefaultValueContext?): List<Node?>? {
        return super.visitDefaultValue(ctx)
    }

    override fun visitModuleDeclaration(ctx: JavaParser.ModuleDeclarationContext?): List<Node?>? {
        return super.visitModuleDeclaration(ctx)
    }

    override fun visitModuleBody(ctx: JavaParser.ModuleBodyContext?): List<Node?>? {
        return super.visitModuleBody(ctx)
    }

    override fun visitModuleDirective(ctx: JavaParser.ModuleDirectiveContext?): List<Node?>? {
        return super.visitModuleDirective(ctx)
    }

    override fun visitRequiresModifier(ctx: JavaParser.RequiresModifierContext?): List<Node?>? {
        return super.visitRequiresModifier(ctx)
    }

    override fun visitRecordDeclaration(ctx: JavaParser.RecordDeclarationContext?): List<Node?>? {
        return super.visitRecordDeclaration(ctx)
    }

    override fun visitRecordHeader(ctx: JavaParser.RecordHeaderContext?): List<Node?>? {
        return super.visitRecordHeader(ctx)
    }

    override fun visitRecordComponentList(ctx: JavaParser.RecordComponentListContext?): List<Node?>? {
        return super.visitRecordComponentList(ctx)
    }

    override fun visitRecordComponent(ctx: JavaParser.RecordComponentContext?): List<Node?>? {
        return super.visitRecordComponent(ctx)
    }

    override fun visitRecordBody(ctx: JavaParser.RecordBodyContext?): List<Node?>? {
        return super.visitRecordBody(ctx)
    }

    override fun visitBlock(ctx: JavaParser.BlockContext?): List<Node?>? {
        val body = currentDoc.createElement("Body")
        ctx?.blockStatement()?.mapNotNull { this.visitBlockStatement(it) }?.forEach { body.appendChild(it) }
        return body.returnValue()
    }

    override fun visitBlockStatement(ctx: JavaParser.BlockStatementContext?): List<Node?>? {
        return super.visitBlockStatement(ctx)
    }

    override fun visitLocalVariableDeclaration(ctx: JavaParser.LocalVariableDeclarationContext?): List<Node?>? {
        return super.visitLocalVariableDeclaration(ctx)
    }

    /**
     * @return Text node
     */
    override fun visitIdentifier(ctx: JavaParser.IdentifierContext?): List<Node?>? {
        return currentDoc.createTextNode(ctx?.text).returnValue()
    }

    /**
     * @return Text node
     */
    override fun visitTypeIdentifier(ctx: JavaParser.TypeIdentifierContext?): List<Node?>? {
        return currentDoc.createTextNode(ctx?.text).returnValue()
    }

    override fun visitLocalTypeDeclaration(ctx: JavaParser.LocalTypeDeclarationContext?): List<Node?>? {
        return super.visitLocalTypeDeclaration(ctx)
    }

    override fun visitStatement(ctx: JavaParser.StatementContext?): List<Node?>? {
        return super.visitStatement(ctx)
    }

    override fun visitCatchClause(ctx: JavaParser.CatchClauseContext?): List<Node?>? {
        return super.visitCatchClause(ctx)
    }

    override fun visitCatchType(ctx: JavaParser.CatchTypeContext?): List<Node?>? {
        return super.visitCatchType(ctx)
    }

    override fun visitFinallyBlock(ctx: JavaParser.FinallyBlockContext?): List<Node?>? {
        return super.visitFinallyBlock(ctx)
    }

    override fun visitResourceSpecification(ctx: JavaParser.ResourceSpecificationContext?): List<Node?>? {
        return super.visitResourceSpecification(ctx)
    }

    override fun visitResources(ctx: JavaParser.ResourcesContext?): List<Node?>? {
        return super.visitResources(ctx)
    }

    override fun visitResource(ctx: JavaParser.ResourceContext?): List<Node?>? {
        return super.visitResource(ctx)
    }

    override fun visitSwitchBlockStatementGroup(ctx: JavaParser.SwitchBlockStatementGroupContext?): List<Node?>? {
        return super.visitSwitchBlockStatementGroup(ctx)
    }

    override fun visitSwitchLabel(ctx: JavaParser.SwitchLabelContext?): List<Node?>? {
        return super.visitSwitchLabel(ctx)
    }

    override fun visitForControl(ctx: JavaParser.ForControlContext?): List<Node?>? {
        return super.visitForControl(ctx)
    }

    override fun visitForInit(ctx: JavaParser.ForInitContext?): List<Node?>? {
        return super.visitForInit(ctx)
    }

    override fun visitEnhancedForControl(ctx: JavaParser.EnhancedForControlContext?): List<Node?>? {
        return super.visitEnhancedForControl(ctx)
    }

    override fun visitParExpression(ctx: JavaParser.ParExpressionContext?): List<Node?>? {
        return super.visitParExpression(ctx)
    }

    override fun visitExpressionList(ctx: JavaParser.ExpressionListContext?): List<Node?>? {
        return super.visitExpressionList(ctx)
    }

    override fun visitMethodCall(ctx: JavaParser.MethodCallContext?): List<Node?>? {
        return super.visitMethodCall(ctx)
    }

    override fun visitExpression(ctx: JavaParser.ExpressionContext?): List<Node?>? {
        return super.visitExpression(ctx)
    }

    override fun visitPattern(ctx: JavaParser.PatternContext?): List<Node?>? {
        return super.visitPattern(ctx)
    }

    override fun visitLambdaExpression(ctx: JavaParser.LambdaExpressionContext?): List<Node?>? {
        return super.visitLambdaExpression(ctx)
    }

    override fun visitLambdaParameters(ctx: JavaParser.LambdaParametersContext?): List<Node?>? {
        return super.visitLambdaParameters(ctx)
    }

    override fun visitLambdaBody(ctx: JavaParser.LambdaBodyContext?): List<Node?>? {
        return super.visitLambdaBody(ctx)
    }

    override fun visitPrimary(ctx: JavaParser.PrimaryContext?): List<Node?>? {
        return super.visitPrimary(ctx)
    }

    override fun visitSwitchExpression(ctx: JavaParser.SwitchExpressionContext?): List<Node?>? {
        return super.visitSwitchExpression(ctx)
    }

    override fun visitSwitchLabeledRule(ctx: JavaParser.SwitchLabeledRuleContext?): List<Node?>? {
        return super.visitSwitchLabeledRule(ctx)
    }

    override fun visitGuardedPattern(ctx: JavaParser.GuardedPatternContext?): List<Node?>? {
        return super.visitGuardedPattern(ctx)
    }

    override fun visitSwitchRuleOutcome(ctx: JavaParser.SwitchRuleOutcomeContext?): List<Node?>? {
        return super.visitSwitchRuleOutcome(ctx)
    }

    override fun visitClassType(ctx: JavaParser.ClassTypeContext?): List<Node?>? {
        return super.visitClassType(ctx)
    }

    override fun visitCreator(ctx: JavaParser.CreatorContext?): List<Node?>? {
        return super.visitCreator(ctx)
    }

    override fun visitCreatedName(ctx: JavaParser.CreatedNameContext?): List<Node?>? {
        return super.visitCreatedName(ctx)
    }

    override fun visitInnerCreator(ctx: JavaParser.InnerCreatorContext?): List<Node?>? {
        return super.visitInnerCreator(ctx)
    }

    override fun visitArrayCreatorRest(ctx: JavaParser.ArrayCreatorRestContext?): List<Node?>? {
        return super.visitArrayCreatorRest(ctx)
    }

    override fun visitClassCreatorRest(ctx: JavaParser.ClassCreatorRestContext?): List<Node?>? {
        return super.visitClassCreatorRest(ctx)
    }

    override fun visitExplicitGenericInvocation(ctx: JavaParser.ExplicitGenericInvocationContext?): List<Node?>? {
        return super.visitExplicitGenericInvocation(ctx)
    }

    override fun visitTypeArgumentsOrDiamond(ctx: JavaParser.TypeArgumentsOrDiamondContext?): List<Node?>? {
        return super.visitTypeArgumentsOrDiamond(ctx)
    }

    override fun visitNonWildcardTypeArgumentsOrDiamond(ctx: JavaParser.NonWildcardTypeArgumentsOrDiamondContext?): List<Node?>? {
        return super.visitNonWildcardTypeArgumentsOrDiamond(ctx)
    }

    override fun visitNonWildcardTypeArguments(ctx: JavaParser.NonWildcardTypeArgumentsContext?): List<Node?>? {
        return super.visitNonWildcardTypeArguments(ctx)
    }

    override fun visitTypeList(ctx: JavaParser.TypeListContext?): List<Node?>? {
        return super.visitTypeList(ctx)
    }

    /**
     * @return Text node
     */
    override fun visitTypeType(ctx: JavaParser.TypeTypeContext?): List<Node?>? {
        var typeName: Text? = null

        ctx?.classOrInterfaceType()?.let { typeName = visitClassOrInterfaceType(it)?.first() as Text }
        ctx?.primitiveType()?.let { typeName = visitPrimitiveType(it)?.first() as Text }

        return typeName?.returnValue()
    }

    /**
     * @return Text node
     */
    override fun visitPrimitiveType(ctx: JavaParser.PrimitiveTypeContext?): List<Node?>? {
        return currentDoc.createTextNode(ctx?.text).returnValue()
    }

    override fun visitTypeArguments(ctx: JavaParser.TypeArgumentsContext?): List<Node?>? {
        return super.visitTypeArguments(ctx)
    }

    override fun visitSuperSuffix(ctx: JavaParser.SuperSuffixContext?): List<Node?>? {
        return super.visitSuperSuffix(ctx)
    }

    override fun visitExplicitGenericInvocationSuffix(ctx: JavaParser.ExplicitGenericInvocationSuffixContext?): List<Node?>? {
        return super.visitExplicitGenericInvocationSuffix(ctx)
    }

    override fun visitArguments(ctx: JavaParser.ArgumentsContext?): List<Node?>? {
        return super.visitArguments(ctx)
    }
}

private fun Node.appendChild(node: List<Node?>?): Node {
    node?.filterNotNull()?.forEach { this.appendChild(it) }
    return this
}

private fun Node.returnValue(): List<Node?> {
    return listOf(this)
}
