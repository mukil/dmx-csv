# DeepaMehta 4 CSV Importer

simple CSV importer to update flat topic compositions

![screenshot](https://github.com/dgf/dm4-csv/raw/master/screenshot.png)

limitations:

 * delimiter: ```|``` (default)
 * data type: Text

## Requirements

 * [DeepaMehta 4](http://github.com/jri/deepamehta) 4.1-SNAPSHOT
 * Write access to dm4.filerepo.path (see DM4 config file)

## Usage

the plugin adds an *Import CSV* action for topic types

format desciption:

 * topic URI prefix as the first entry (row: 0, col: 0)
 * all direct child topics are the remaining header columns

### import some topics from a CSV file

to create some *Note* topics use a CSV file like the following:

```
csv.example | dm4.notes.title | dm4.notes.text
one         | check this      | with content
two         | check this too  | and with another content
```

upload the file with the *Import CSV* action of the *Note* topic type

an import of some *Web Resource* topics could look like this:

```
deep.web | dm4.webbrowser.url        | dm4.webbrowser.web_resource_description
site     | http://www.deepamehta.de  | <h1>DeepaMehta</h1><p class="slogan">Cope With Complexity</p>
demo     | http://demo.deepamehta.de | <h1>Demo Server</h1><p>try it now</p>
ci       | http://ci.deepamehta.de   | <h1>Continuous Integration</h1><p>fresh nightly builds</p>
```


### update topics

to update all instances of a topic type just import the modified CSV file again,
but be aware of these rules:

 * existing topic is updated by a URI match
 * row without a matching topic URI creates a new topic instance
 * existing topic instance with a type URI and no actual row match is deleted

