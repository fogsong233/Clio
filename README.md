![](./github/title.jpg)
## Clio
A QQ group auto logger of the discussion among people
which can generate the document to the group file.  
Powered by [Mirai](https://github.com/mamoe/mirai)  
*maybe you should have the operator power!*
> how to use it?

You can just download the .jar release and add it to you plugins.  
**And** remember, you need to add a config file to ./data/tech.fogsong.qqlogger.clio/  
name: config.properties
```properties
# we will change it automatically! dont change it!
doc_numbers=1
greed_mode=true
# the active key of the plugin use, you'd better keep #
key=\#
#replace your group id and your bot id
enable_group_ids=123456,2424244,22424
bot_qq_id=10001
```
You can disable greed mode to just log the message end with `#index`, to the problem `index`,
when multiple question is opening.
> How to use it in the group?  

**Warning**: because of Mirai's wrong parse, the plugin did not support image, so that we **highly recommend** everyone who take part in the question and the discussion
upload your image by `file`, and we support it.
### 1. open a question:
use the default key `#` as example:
you can send a message like that:
```
## hello, i have some question: xxxxxxxxxxxxxxxxxxxxx...
```
or multiple message:
```
###
hello.
i have a question.
[image]
[image]
how to....
....
...
###
(the the bot will send "log complete")
```
### 2. dicusstion
After a question being logged, you can just discussion without other command.  
**But** if you disable the greed mode or there are two or more question opening the same time,
you can add `#[number]` in your reply end.  
Also, all file of image will be logged without manual process.  
For example, if there are two question, this plugin will auto index it with `1,2,3,4...`.(And e.g. `2` is closed,
`3` will be indexed `2` and so on)  
If you only want to share your thoughts in question `3`, you can say:
```properties
i have some.............(a lot of words)#3
```
The msg will only be logged in question `3`.  
### 3. End the question
just command `#end[index]` e.g. `#end1` as usual.  
Wait a minute, the doc will be uploaded into a directory named "原始记录文件",
with name including date.



