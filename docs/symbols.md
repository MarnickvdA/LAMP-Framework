# Symbol Extraction
The Semantic Tree is the same as our Metamodel, but it contains some specialized information for retrieving the right 
semantic information from the elements within our metamodel. As you might already know, the metamodel is saved as 1 
folder containing all files required for our parser to import the whole project. The tree structure of the project looks
missing, but it isn't! Due to naming conventions, we can extract the right scoping information, regarding component
level scopes, from the file names. Within those files, scoping on module level is facilitated. Therefore, when we are
looking for references within the metamodel done to the outside, we can traverse up from the calling element towards the
root of our metamodel AST.


### Example
For example, we do a UnitCall referencing a Unit that is located within another class. Our metamodel is 'dumb', while
the compiler that processed the code was smart. Therefore, we have to do the same steps as the language-specific
compiler did.
```
We will keep looking until we have found the source.

"The call was done within a Unit source element."

    Look within the Unit scope if there is a LocalDeclaration with the reference name of the UnitCall.
    Look within the Unit's parameters if it is referencing the thing we need. 
    Look within the Module scope if there are Unit's with the name we require. 
    
    For each Module in our imports (explicit imports and implicit imports):
        look if there is a unit with the correct reference name.
   
    Not found? We have a limitation within our metamodel, have a look if it is fixable.
```

## The Pattern
From this specific example, we can see a pattern emerging. We are constantly trying to move one level up in the scope 
and search there for the element we seek. In our metamodel, elements have no knowledge about their parent, which is why 
we need to implement that logic within our SemanticTreeVisitor. This visitor will be responsible for creating a mapping
from the specific metamodel source elements to their parent element. This mapping will allow us to find elements and 
search for references that they can reach.

### Cases
So, with that known, what type of patterns are possible? For that, we must analyse the possibilities that we can extract
from our metamodel. Within our metamodel, only Declarable types can be interacted with. We can distinguish between several 
types of interaction, which are property access, unit calls and property assignments. All interactions with Declarable 
types can happen through nested Declarable types. 

#### Straight forward cases
```java
class Example {                 // Module declaration
    int y = 1;                  // Property declaration with an assignment

    int getTheAnswer() {        // Unit declaration
        int x = y + 1;          // Property declaration with an initializer (i.e. Assignment), 'y' is an Identifier that is 'accessed'
        x = theAnswer();        // Assignment of x. 'theAnswer' is the reference to a Unit, this is a UnitCall
        return x;               
    }

    int theAnswer() {           // Unit declaration
        return Constant.ANSWER; // 'ANSWER' is an Identifier (referencing a Property), accessed through 'Constant' also an Identifier (referencing a Module)
    }

    static class Constant {     // Module declaration
        int ANSWER = 42;        // Property declaration
    }
}
```

#### Referencing via a module outside the module
It can be the case that the Identifier is referencing to a part of the application which is not visible within the module.
In that case, we must look at the imports to see if our reference is situated over there. In this example, Constant is 
an explicit import.

```java
import nl.utwente.utilities.Constant;

class Example {
    int theAnswer() {           // Unit declaration
        return Constant.ANSWER; // 'ANSWER' is an Identifier reference with a Module that outside of the module.
    }
}
```

Constant could also be an implicit import. See the example below for an idea how that looks.
```java
import nl.utwente.views.*;   // A component that we must iterate through also to find our Module reference.
import nl.utwente.utilities.*; // Our Module is situated in this component.

class Example {
    int theAnswer() {           // Unit declaration
        return Constant.ANSWER; // 'ANSWER' is an Identifier reference with a Module that outside of the module.
    }
}
```
In this case, we must iterate through all Module declarations made in `nl.utwente.views` and `nl.utwente.utitilies` until
we have found our referenced Module. 

#### Reference via unit call return value
It can be the case that the UnitCall returns a Declarable value. Our metamodel does not contain type information (yet),
so that means that this case cannot be covered by our semantic model. This means that our metrics regarding semantic
references via UnitCall return types will not be included in the result. 

[//]: # (TODO Include this conclusion in thesis. Type was out of scope due to Type Parameterization.)

```java
import nl.utwente.utilities.ExampleFactory;

class Example {
    NumberFactory getFactory() {            // Unit returning a Declarable type
        return new ExampleFactory();        // UnitCall to 'constructor' of ExampleFactory
    }
    
    int theAnswer() {                       // Unit declaration returning 
        return getFactory().build("42");    // 'build' is a UnitCall to NumberFactory, but we do not have that information.
    }
}
```