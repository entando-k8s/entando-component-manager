# Guide to create a new Processor
This guide will explain how to create new processors on the Digital Exchange.

## What is a processor?
A Processor is a Java Interface to handle components, either by registering or uninstalling them.

## How to process a new Component?
In order to register and deregister components you will need to implement only one interface and that is it.

>- Important: The class must be annotated with `@Service` to be injected on the Spring Context.

### ComponentProcessor
The component processor has a few methods:

#### `ComponentProcessor.process`
This method will process the ComponentDescriptor and should return a list of Installabled (I will talk about installable later).

#### `ComponentProcessor.shouldProcess`
This method will the checked whenever a component is updated or removed. The method must return true if the class is capable to manage the type.

#### `ComponentProcessor.uninstall`
This method will be executed when the component is being uninstalled, the uninstallation process of the component itself is totally responsibility of this class.

#### `ComponentProcessor.update`
This method doesn't exist yet, but will be executed when the component changed and must be updated.

### Installable
The installable is a class which has the responsibility to register a component. It has a few methods to implement.

#### `Installable.install`
This method must return a `CompletableFuture` of the installation process.

#### `Installable.getComponentType`
Must return the component type of the registration.

#### `Installable.getName`
Must return the component name, it will be treated as it's identifier.
