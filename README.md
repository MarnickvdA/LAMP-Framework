# Language-Agnostic Multi-Paradigm Code Quality Assurance Framework
This git repository contains the prototype of the LAMP Framework.

### System requirements
**System libraries**:
- Graphviz

**Intellij plugins**:
- Jakarta EE (requires Ultimate)
- PlantUML

### Terminology
- Project
- Component
- Module
- Unit

### Metamodel (XML Schema)
The metamodel is created using the XML Schema Definition (XSD) language.
The metamodel XML schema is located in `src/main/resources`.

Additionally, the `metamodel-bindings.xjb` file is used to define global bindings to the schema, such as the package name that will be defined for every generated class.

### Code Generation (JAXB) 
We use the visitor pattern, similar to ANTLR, to traverse the XML documents within our generated code. 
The visitor base class is generated using the library [jaxb-visitor](https://github.com/massfords/jaxb-visitor).
Using the command `mvn jaxb30:generate`, we can generate the Metamodel classes and the Visitor base classes.

The generated metamodel code is located at `target/generated-sources/xjc/nl.utwente.student.metamodel`.
The generated visitor is located at `target/generated-sources/xjc/nl.utwente.student.visitor`.

### Application Flow

```
(1)             Project Code                =(using)=>  Compiler API         =(outputs)=>    An Abstract Syntax Tree model per module
(2) For every:  AST model                   =(using)=>  XML schema           =(outputs)=>    Metamodel XML document
(3) For every:  Metamodel XML document      =(using)=>  JAXB Unmarshalling   =(outputs)=>    Module Metamodel instance
(4) For every:  Module Metamodel instance   =(using)=>  JAXB Visitor         =(outputs)=>    Metrics
```