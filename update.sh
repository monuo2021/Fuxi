# 如果没有消息后缀，默认提交信息为 `:pencil: update content`
#!/bin/bash
info=$1
if ["$info" = ""];
then info=":pencil: update content"
fi
git add .
git commit -m "$info"
git push
