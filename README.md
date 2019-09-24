# An Annotation Processor to manage I18n 

### Goal

The main goal is to avoid keep creating boilerplate code to dealing with I18n.

### Usage
You just need to create an interface with methods that returns a string. <br>
Then you have to annotate the interface with `@MessageBundle` and all the desired methods with one or more `@Message` informing the message template:

```
@MessageBundle
public interface SourceResourceBundleWithTwoMethods {
    @Message(" worked {0} ! ")
    public String m1(String pZero);

    @Message(" worked {0} {1}!")
    public String m2(String pZero, String pOne);
}
```
In the example below both methods were tagged with default locale, that is `en-US`. <br>But you can also choose a specific locale as in the example below:

```
@MessageBundle
public interface SourceResourceBundleWithTwoMethods {
    @Message(value = "funcionou {0} ! ", locale = "pt-BR")
    @Message(value = "worked {0} ! ", locale = "en-US")
    public String m1(String pZero);

    @Message(value = "worked {0} {1}!", locale = "en-US")
    @Message(value = "funcionou {0} {1}!", locale = "pt-BR")
    public String m2(String pZero, String pOne);
}
```

The annotation processor will create a concrete class that implements your interface and all properties files for each location found in the annotations. 