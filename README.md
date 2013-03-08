# DeepaMehta 4 CSV Importer

simple CSV importer to update flat topic compositions

limitations:

 * delimiter: ```|``` (default)
 * data type: Text

## Requirements

 * [DeepaMehta 4](http://github.com/jri/deepamehta) 4.1-SNAPSHOT

## Usage

the plugin adds an *Import CSV* action for topic types

format desciption:

 * topic URI prefix as the first entry (row: 0, col: 0)
 * all direct child topics are the remaining header columns

### import some notes from a CSV file

to create some *Note* topics use a CSV file like the following:

```
csv.example | dm4.notes.title | dm4.notes.text
one         | check this      | with content
two         | check this too  | and with another content
```

upload the file with the the *Import CSV* action of the *Note* topic type

### update topics

to update all instances of a topic type just import the modified CSV file again,
but be aware of these rules:

 * existing topic is updated by a URI match
 * row without a matching topic URI creates a new topic instance
 * existing topic instance with a type URI and no actual row match is deleted

