/**
 * Created by Leonid on 1/17/14.
 */
define([
    "js/OAuthUtil",
    "dojo/store/Memory",
    "dijit/registry",
    "js/CategoryUtil"
], function (oAuthUtil,Memory,registry,categoryUtil)
{
    var taskUtil = {
        synchronize: function(){
            localStorage.args = "";
            localStorage.actiontype = "";
            if(window.gce_loaded)
                this.upload(0);
            else
                setTimeout(this.synchronize(),10000);
        },
        initTaskStore: function(){
            taskStore = new Memory();
            var jsObj = eval(localStorage.taskData);
            console.log(jsObj);
            //var arr = jsObj.categories;
            //console.log(arr);
            if(typeof(jsObj)!='undefined'){
                for(var i=0;i<jsObj.length;i++){
                    var tmp = new Object();
                    tmp.id = jsObj[i].id;
                    tmp.status = jsObj[i].status;
                    tmp.category = jsObj[i].category;
                    tmp.title = decodeURI(jsObj[i].title);
                    tmp.description = decodeURI(jsObj[i].description);
                    tmp.local_timestamp = jsObj[i].local_timestamp;
                    tmp.server_timestamp = jsObj[i].server_timestamp;
                    taskStore.add(tmp);
                }
            }
        },
        upload: function(index){
            var self = this;
            if(index>=taskStore.data.length){
                self.read();
                return;
            }
            console.log("lt = "+taskStore.data[index].local_timestamp);
            console.log("st = "+taskStore.data[index].server_timestamp);
            if(taskStore.data[index].local_timestamp!=taskStore.data[index].server_timestamp){
                if(typeof(taskStore.data[index].server_timestamp)=="undefined"){//if new task
                    if(islocal)
                        oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                            var arg = {title:taskStore.data[index].title,description:taskStore.data[index].description,category:taskStore.data[index].category,status:taskStore.data[index].status};
                            var insertTask = gapi.client.task.insert(arg);
                            insertTask.execute(function (data){
                                console.log("task = "+JSON.stringify(data));
                                index++;
                                self.upload(index);
                            });
                        }));
                    else
                        gapi.client.load('task', apiVersion, function(yy) {
                            console.log("task API loaded. "+JSON.stringify(yy));
                            console.log("task API loaded. "+gapi.client.task);

                            gapi.auth.authorize({client_id: clientId,
                                    scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                                function(res)
                                {
                                    console.log(res);
                                    var arg = {title:taskStore.data[index].title,description:taskStore.data[index].description,category:taskStore.data[index].category,status:taskStore.data[index].status};
                                    var insertTask = gapi.client.task.insert(arg);
                                    insertTask.execute(function (data){
                                        console.log("task = "+JSON.stringify(data));
                                        index++;
                                        self.upload(index);
                                    });
                                });
                        }, ROOT);
                }
                else{//task update
                    var arg = {id:taskStore.data[index].id,title:taskStore.data[index].title,description:taskStore.data[index].description,category:taskStore.data[index].category,status:taskStore.data[index].status,client_copy_timestamp:taskStore.data[index].server_timestamp};
                    if(islocal)
                        oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                            console.log(arg);
                            var updateTask = gapi.client.task.update(arg);
                            updateTask.execute(function (data){
                                console.log("task = "+JSON.stringify(data));
                                if(data.id){
                                    taskStore.remove(taskStore.data[index].id);
                                    var category = "";
                                    /*for(var i=0;i<categoryStore.data.length;i++)
                                    if(data.category == categoryStore.data[i].name)
                                        category = categoryStore.data[i].id;*/
                                    category = data.category;
                                    taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                    localStorage.selectedTaskID = data.id;
                                    /*self.refreshTaskList(localStorage.taskListType);
                                    refreshCategoryList(localStorage.selectedCategoryID_);
                                    self.refreshStatusList();
                                    self.getContextList();
                                    self.saveTaskToLocalStorage();*/
                                }
                                else{
                                    //self.updateTask(localStorage.selectedTaskID);
                                    /*self.refreshTaskList(localStorage.taskListType);
                                    refreshCategoryList(localStorage.selectedCategoryID_);
                                    self.refreshStatusList();
                                    self.getContextList();
                                    self.saveTaskToLocalStorage();*/
                                }
                            });
                        }));
                    else
                        gapi.client.load('task', apiVersion, function(yy) {
                            console.log("task API loaded. "+JSON.stringify(yy));
                            console.log("task API loaded. "+gapi.client.task);

                            gapi.auth.authorize({client_id: clientId,
                                    scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                                function(res)
                                {
                                    console.log(res);
                                    console.log(arg);
                                    var updateTask = gapi.client.task.update(arg);
                                    updateTask.execute(function (data){
                                        console.log("task = "+JSON.stringify(data));
                                        if(data.id){
                                            taskStore.remove(taskStore.data[index].id);
                                            var category = "";
                                            /*for(var i=0;i<categoryStore.data.length;i++)
                                            if(data.category == categoryStore.data[i].name)
                                                category = categoryStore.data[i].id;*/
                                            category = data.category;
                                            taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                            localStorage.selectedTaskID = data.id;
                                            self.refreshTaskList(localStorage.taskListType);
                                            refreshCategoryList(localStorage.selectedCategoryID_);
                                            self.refreshStatusList();
                                            self.getContextList();
                                            self.saveTaskToLocalStorage();
                                        }
                                        else{
                                            //self.updateTask(localStorage.selectedTaskID);
                                            self.refreshTaskList(localStorage.taskListType);
                                            refreshCategoryList(localStorage.selectedCategoryID_);
                                            self.refreshStatusList();
                                            self.getContextList();
                                            self.saveTaskToLocalStorage();
                                        }
                                    });
                                });
                        }, ROOT);
                }
            }
            else{
                index++;
                self.upload(index);
            }
        },
        updateTaskToServer: function(){
            var self = this;
            var id = localStorage.selectedTaskID;
            console.log("args="+localStorage.args);
            if(localStorage.args != "" && typeof(localStorage.args)!="undefined"){
                var arg = JSON.parse(localStorage.args);
                if(islocal)
                    oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                        console.log(arg);
                        var updateTask = gapi.client.task.update(arg);
                        updateTask.execute(function (data){
                            console.log("task = "+JSON.stringify(data));
                            if(data.id){
                                taskStore.remove(id);
                                var category = "";
                                /*for(var i=0;i<categoryStore.data.length;i++)
                                 if(data.category == categoryStore.data[i].name)
                                 category = categoryStore.data[i].id;*/
                                category = data.category;
                                taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                localStorage.selectedTaskID = data.id;
                                self.refreshTaskList(localStorage.taskListType);
                                refreshCategoryList(localStorage.selectedCategoryID_);
                                self.refreshStatusList();
                                self.getContextList();
                                self.saveTaskToLocalStorage();
                            }
                            else{
                                //self.updateTask(localStorage.selectedTaskID);
                                self.refreshTaskList(localStorage.taskListType);
                                refreshCategoryList(localStorage.selectedCategoryID_);
                                self.refreshStatusList();
                                self.getContextList();
                                self.saveTaskToLocalStorage();
                            }
                        });
                    }));
                else
                    gapi.client.load('task', apiVersion, function(yy) {
                        console.log("task API loaded. "+JSON.stringify(yy));
                        console.log("task API loaded. "+gapi.client.task);

                        gapi.auth.authorize({client_id: clientId,
                                scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                            function(res)
                            {
                                console.log(res);
                                console.log(arg);
                                var updateTask = gapi.client.task.update(arg);
                                updateTask.execute(function (data){
                                    console.log("task = "+JSON.stringify(data));
                                    if(data.id){
                                        taskStore.remove(id);
                                        var category = "";
                                        /*for(var i=0;i<categoryStore.data.length;i++)
                                         if(data.category == categoryStore.data[i].name)
                                         category = categoryStore.data[i].id;*/
                                        category = data.category;
                                        taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                        localStorage.selectedTaskID = data.id;
                                        self.refreshTaskList(localStorage.taskListType);
                                        refreshCategoryList(localStorage.selectedCategoryID_);
                                        self.refreshStatusList();
                                        self.getContextList();
                                        self.saveTaskToLocalStorage();
                                    }
                                    else{
                                        //self.updateTask(localStorage.selectedTaskID);
                                        self.refreshTaskList(localStorage.taskListType);
                                        refreshCategoryList(localStorage.selectedCategoryID_);
                                        self.refreshStatusList();
                                        self.getContextList();
                                        self.saveTaskToLocalStorage();
                                    }
                                });
                            });
                    }, ROOT);
            }
            localStorage.args = "";
        },
        updateTask: function(id,status,categoryid){
            var self = this;
            var server_timestamp = taskStore.get(id).server_timestamp;
            var task = taskStore.get(id);
            /*if(task.local_timestamp!=task.server_timestamp){
                taskStore.remove(id);
                self.createTask(task.title,task.description,task.category,task.status);
                self.createTaskToServer();
            }
            else{*/

                taskStore.remove(id);
                //var arg = {id:id,title:title,description:encodeURI(description),category:category,status:status,client_copy_timestamp:server_timestamp};
                var arg = {};
                if(localStorage.taskListType=="status"){
                    if(typeof(status)=='undefined'||status==''){
                        taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus,local_timestamp:id,server_timestamp:server_timestamp});
                        arg = {id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus,client_copy_timestamp:server_timestamp};
                    }
                    else{
                        taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:status,local_timestamp:id,server_timestamp:server_timestamp});
                        arg = {id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:status,client_copy_timestamp:server_timestamp};
                    }
                }
                else if(localStorage.taskListType=="category"){
                    if(typeof(categoryid)=='undefined'){
                        taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_,local_timestamp:id,server_timestamp:server_timestamp});
                        arg = {id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_,client_copy_timestamp:server_timestamp};
                        console.log("selected category id1 = "+localStorage.selectedCategoryID_);
                    }
                    else{
                        taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:categoryid,status:localStorage.selectedStatus_,local_timestamp:id,server_timestamp:server_timestamp});
                        arg = {id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:categoryid,status:localStorage.selectedStatus_,client_copy_timestamp:server_timestamp};
                    }
                }
                else if(localStorage.taskListType=="context"){
                    taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_,local_timestamp:id,server_timestamp:server_timestamp});
                    arg = {id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_,client_copy_timestamp:server_timestamp};
                }
                if(arg.category==''){
                    arg = {id:arg.id,title:arg.title,description:arg.description,status:arg.status,client_copy_timestamp:arg.client_copy_timestamp};
                }
                self.refreshTaskList(localStorage.taskListType);
                refreshCategoryList(categoryid);
                self.refreshStatusList();
                self.getContextList();
                self.saveTaskToLocalStorage();
                console.log(arg);
                localStorage.args = JSON.stringify(arg);
                if(typeof(task.server_timestamp)=="undefined"){
                    localStorage.actiontype = "taskInsert";
                }
                else{
                    localStorage.actiontype = "taskUpdate";
                }
                localStorage.selectedTaskID = id;

                /*if(islocal)
                    oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                        console.log(arg);
                        var updateTask = gapi.client.task.update(arg);
                        updateTask.execute(function (data){
                            console.log("task = "+JSON.stringify(data));
                            if(data.id){
                                taskStore.remove(id);
                                var category = "";
                                *//*for(var i=0;i<categoryStore.data.length;i++)
                                    if(data.category == categoryStore.data[i].name)
                                        category = categoryStore.data[i].id;*//*
                                category = data.category;
                                taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                localStorage.selectedTaskID = data.id;
                                self.refreshTaskList(localStorage.taskListType);
                                refreshCategoryList(localStorage.selectedCategoryID_);
                                self.refreshStatusList();
                                self.getContextList();
                                self.saveTaskToLocalStorage();
                            }
                            else{
                                //self.updateTask(localStorage.selectedTaskID);
                                self.refreshTaskList(localStorage.taskListType);
                                refreshCategoryList(localStorage.selectedCategoryID_);
                                self.refreshStatusList();
                                self.getContextList();
                                self.saveTaskToLocalStorage();
                            }
                        });
                    }));
                else
                    gapi.client.load('task', apiVersion, function(yy) {
                        console.log("task API loaded. "+JSON.stringify(yy));
                        console.log("task API loaded. "+gapi.client.task);

                        gapi.auth.authorize({client_id: clientId,
                                scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                            function(res)
                            {
                                console.log(res);
                                console.log(arg);
                                var updateTask = gapi.client.task.update(arg);
                                updateTask.execute(function (data){
                                    console.log("task = "+JSON.stringify(data));
                                    if(data.id){
                                        taskStore.remove(id);
                                        var category = "";
                                        *//*for(var i=0;i<categoryStore.data.length;i++)
                                            if(data.category == categoryStore.data[i].name)
                                                category = categoryStore.data[i].id;*//*
                                        category = data.category;
                                        taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                        localStorage.selectedTaskID = data.id;
                                        self.refreshTaskList(localStorage.taskListType);
                                        refreshCategoryList(localStorage.selectedCategoryID_);
                                        self.refreshStatusList();
                                        self.getContextList();
                                        self.saveTaskToLocalStorage();
                                    }
                                    else{
                                        //self.updateTask(localStorage.selectedTaskID);
                                        self.refreshTaskList(localStorage.taskListType);
                                        refreshCategoryList(localStorage.selectedCategoryID_);
                                        self.refreshStatusList();
                                        self.getContextList();
                                        self.saveTaskToLocalStorage();
                                    }
                                });
                            });
                    }, ROOT);*/
            //}
        },
        refreshTaskList: function(task_list_type){
            document.getElementById("task-list").innerHTML = "";
            var listWidget = registry.byId("task-list");
            registry.byId('plusbtn').set('style','display:block');
            if(task_list_type=="status"){
                for(var i=0;i<categoryStore.data.length;i++){
                    var tmp = taskStore.query({ category: categoryStore.data[i].id,status:localStorage.selectedStatus });
                    if(tmp.length>0){
                        var id = categoryStore.data[i].id;
                        var itemWidget = new dojox.mobile.ListItem({
                            label: "<span style='color:blue;'>"+categoryStore.data[i].name+"</span>",
                            index: categoryStore.data[i].id,
                            icon:'images/book.png',
                            //moveTo: "#",
                            onClick:function( e ){

                            }
                        });
                        listWidget.addChild(itemWidget);
                    }
                    for(var j=0;j<tmp.length;j++){
                        itemWidget = new dojox.mobile.ListItem({
                            label: tmp[j].title,
                            index: tmp[j].id,
                            //icon:'images/book.png',
                            moveTo: "#",
                            onClick:function( e ){
                                moveToEditTaskPage(this.index,localStorage.selectedStatus);
                            }
                        });
                        listWidget.addChild(itemWidget);
                    }
                }
                var tmp = taskStore.query({ category: "",status:localStorage.selectedStatus });
                if(tmp.length>0){
                    var itemWidget = new dojox.mobile.ListItem({
                        label: "<span style='color:blue;'>"+uncategorized+"</span>",
                        //index: categoryStore.data[i].id,
                        icon:'images/book.png',
                        //moveTo: "#",
                        onClick:function( e ){

                        }
                    });
                    listWidget.addChild(itemWidget);
                    for(var j=0;j<tmp.length;j++){
                        itemWidget = new dojox.mobile.ListItem({
                            label: tmp[j].title,
                            index: tmp[j].id,
                            //icon:'images/book.png',
                            moveTo: "#",
                            onClick:function( e ){
                                moveToEditTaskPage(this.index,localStorage.selectedStatus);
                            }
                        });
                        listWidget.addChild(itemWidget);
                    }
                }
            }
            else if(task_list_type=="category"){
                console.log("category = "+localStorage.selectedCategoryID);
                for (var i=0;i<statuslist.length ;i++ ){
                    var tmp = taskStore.query({ category: localStorage.selectedCategoryID,status:statuslist[i] });
                    if(tmp.length>0){
                        var itemWidget = new dojox.mobile.ListItem({
                            label: "<span style='color:blue;'>"+statuslist[i]+"</span>",
                            index: statuslist[i],
                            icon:'images/'+iconarray[i],
                            //moveTo: "#",
                            onClick:function( e ){}
                        });
                        listWidget.addChild(itemWidget);
                    }
                    for(var j=0;j<tmp.length;j++){
                        itemWidget = new dojox.mobile.ListItem({
                            label: tmp[j].title,
                            index: tmp[j].id,
                            status: statuslist[i],
                            //icon:'images/book.png',
                            moveTo: "#",
                            onClick:function( e ){
                                moveToEditTaskPage(this.index,this.status);
                            }
                        });
                        listWidget.addChild(itemWidget);
                    }
                }

                var tmp = taskStore.query({ category: localStorage.selectedCategoryID,status:"" });
                console.log(tmp);
                if(tmp.length>0){
                    var itemWidget = new dojox.mobile.ListItem({
                        label: "<span style='color:blue;'>"+nostatus+"</span>",
                        //index: categoryStore.data[i].id,
                        icon:'images/book.png',
                        //moveTo: "#",
                        onClick:function( e ){

                        }
                    });
                    listWidget.addChild(itemWidget);
                    for(var j=0;j<tmp.length;j++){
                        itemWidget = new dojox.mobile.ListItem({
                            label: tmp[j].title,
                            index: tmp[j].id,
                            //icon:'images/book.png',
                            moveTo: "#",
                            onClick:function( e ){
                                moveToEditTaskPage(this.index);
                            }
                        });
                        listWidget.addChild(itemWidget);
                    }
                }
            }
            else if(task_list_type=="context"){
                registry.byId('plusbtn').set('style','display:none');
                var tasklist = [];
                for(var i=0;i<taskStore.data.length;i++){
                    var title = taskStore.data[i].title;
                    var description = taskStore.data[i].description;
                    if(title.indexOf(localStorage.selectedContext)!=-1 || description.indexOf(localStorage.selectedContext)!=-1){
                        tasklist.push(taskStore.data[i]);
                    }
                }
                var statuslist_ = [];
                for(var i=0;i<statuslist.length;i++){
                    var flag = false;
                    for(var j=0;j<tasklist.length;j++){
                        if(tasklist[j].status==statuslist[i]){
                            flag = true;
                        }
                    }
                    if(flag==true){
                        statuslist_.push(statuslist[i]);
                    }
                }
                for(var j=0;j<statuslist_.length;j++){
                    var itemWidget = new dojox.mobile.ListItem({
                        label: "<span style='color:blue;'>"+statuslist_[j]+"</span>",
                        icon:'images/book.png',
                        moveTo: "",
                        onClick:function( e ){}
                    });
                    listWidget.addChild(itemWidget);
                    ///
                    for(var i=0;i<tasklist.length;i++){
                        var tmp = tasklist[i];
                        if(tmp.status == statuslist_[j]){
                            itemWidget = new dojox.mobile.ListItem({
                                label: tmp.title,
                                index: tmp.id,
                                status: tmp.status,
                                //icon:'images/book.png',
                                moveTo: "#",
                                onClick:function( e ){
                                    moveToEditTaskPage(this.index,this.status);
                                }
                            });
                            listWidget.addChild(itemWidget);
                        }
                    }
                    ///
                }
            }
        },
        saveTaskToLocalStorage: function(){
            var taskstr = "{tasks:[";
            for(var i=0;i<taskStore.data.length;i++){
                taskstr +="{'id':'"+taskStore.data[i].id+"','status':'"+taskStore.data[i].status+"','category':'"+taskStore.data[i].category+"','title':'"+encodeURI(taskStore.data[i].title)+"','local_timestamp':'"+taskStore.data[i].local_timestamp+"','server_timestamp':'"+taskStore.data[i].server_timestamp+"','description':'"+encodeURI(taskStore.data[i].description)+"'}";
                if(i!=(taskStore.data.length-1))
                    taskstr += ",";
            }
            taskstr += "]}";
            localStorage.taskData = taskstr;
        },
        createTaskToServer: function(){
            var self = this;
            var id = localStorage.selectedTaskID;
            if(localStorage.args != "" && typeof(localStorage.args)!="undefined"){
                var arg = JSON.parse(localStorage.args);
                if(islocal)
                    oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                        var insertTask = gapi.client.task.insert(arg);
                        insertTask.execute(function (data){
                            console.log("task = "+JSON.stringify(data));
                            if(data.id){
                                taskStore.remove(id);
                                var category = "";
                                /*for(var i=0;i<categoryStore.data.length;i++)
                                    if(data.category == categoryStore.data[i].name)
                                        category = categoryStore.data[i].id;*/
                                if(typeof(data.category) == "undefined")
                                    category = "";
                                else
                                    category = data.category;
                                taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                localStorage.selectedTaskID = data.id;
                            }
                            //self.updateTask(localStorage.selectedTaskID);
                            self.refreshTaskList(localStorage.taskListType);
                            refreshCategoryList(localStorage.selectedCategoryID_);
                            self.refreshStatusList();
                            self.getContextList();
                        });
                    }));
                else
                    gapi.client.load('task', apiVersion, function(yy) {
                        console.log("task API loaded. "+JSON.stringify(yy));
                        console.log("task API loaded. "+gapi.client.task);
                        gapi.auth.authorize({client_id: clientId,
                                scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                            function(res)
                            {
                                console.log(res);
                                //var arg = {title:title,description:encodeURI(description),category:category,status:status};
                                var insertTask = gapi.client.task.insert(arg);
                                insertTask.execute(function (data){
                                    console.log("task = "+JSON.stringify(data));
                                    if(data.id){
                                        taskStore.remove(id);
                                        var category = "";
                                        /*for(var i=0;i<categoryStore.data.length;i++)
                                            if(data.category == categoryStore.data[i].name)
                                                category = categoryStore.data[i].id;*/
                                        category = data.category;
                                        if(typeof(data.category) == "undefined")
                                            category = "";
                                        else
                                            category = data.category;
                                        taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                        localStorage.selectedTaskID = data.id;
                                    }
                                    //self.updateTask(localStorage.selectedTaskID);
                                    self.refreshTaskList(localStorage.taskListType);
                                    refreshCategoryList(localStorage.selectedCategoryID_);
                                    self.refreshStatusList();
                                    self.getContextList();
                                });
                            });
                    }, ROOT);
            }
            localStorage.args = "";
        },
        createTask: function(title,description,category,status){
            var self = this;
            var id = (new Date()).getTime();
            console.log("category = "+category);
            taskStore.add({id:id,title:title,description:description,category:category,status:status,local_timestamp:id});
            for(var i=0;i<taskStore.data.length;i++)
                console.log(i+" = "+taskStore.data[i].server_timestamp);
            self.refreshTaskList(localStorage.taskListType);
            refreshCategoryList(localStorage.selectedCategoryID_);
            self.refreshStatusList();
            self.getContextList();
            self.saveTaskToLocalStorage();
            for(var i=0;i<taskStore.data.length;i++)
                console.log(i+" = "+taskStore.data[i].server_timestamp);
            localStorage.selectedTaskID = id;

            var arg="";
            if(category==""){
                arg = {title:title,description:encodeURI(description),status:status};
            }
            else
                arg = {title:title,description:encodeURI(description),category:category,status:status};
            localStorage.args = JSON.stringify(arg);
            localStorage.actiontype = "taskInsert";

            /*if(islocal)
                oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                    var insertTask = gapi.client.task.insert(arg);
                    insertTask.execute(function (data){
                        console.log("task = "+JSON.stringify(data));
                        if(data.id){
                            taskStore.remove(id);
                            var category = "";
                            for(var i=0;i<categoryStore.data.length;i++)
                                if(data.category == categoryStore.data[i].name)
                                    category = categoryStore.data[i].id;
                            taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                            localStorage.selectedTaskID = data.id;
                        }
                        //self.updateTask(localStorage.selectedTaskID);
                        self.refreshTaskList(localStorage.taskListType);
                        refreshCategoryList(localStorage.selectedCategoryID_);
                        self.refreshStatusList();
                        self.getContextList();
                    });
                }));
            else
                gapi.client.load('task', apiVersion, function(yy) {
                    console.log("task API loaded. "+JSON.stringify(yy));
                    console.log("task API loaded. "+gapi.client.task);
                    gapi.auth.authorize({client_id: clientId,
                            scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                        function(res)
                        {
                            console.log(res);
                            //var arg = {title:title,description:encodeURI(description),category:category,status:status};
                            var insertTask = gapi.client.task.insert(arg);
                            insertTask.execute(function (data){
                                console.log("task = "+JSON.stringify(data));
                                if(data.id){
                                    taskStore.remove(id);
                                    var category = "";
                                    for(var i=0;i<categoryStore.data.length;i++)
                                        if(data.category == categoryStore.data[i].name)
                                            category = categoryStore.data[i].id;
                                    taskStore.add({id:data.id,title:data.title,local_timestamp:data.last_updated,server_timestamp:data.last_updated,status:data.status,category:category,description:encodeURI(data.description)});
                                    localStorage.selectedTaskID = data.id;
                                }
                                //self.updateTask(localStorage.selectedTaskID);
                                self.refreshTaskList(localStorage.taskListType);
                                refreshCategoryList(localStorage.selectedCategoryID_);
                                self.refreshStatusList();
                                self.getContextList();
                            });
                        });
                }, ROOT);*/
        },
        read:function(){
            var self = this;
            if(islocal)
                oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                    var getTaskList = gapi.client.task.list();
                    getTaskList.execute(function (data){
                        console.log("task lists = "+JSON.stringify(data));
                        if(data.task_list)
                            self.updateTaskStore(data.task_list);
                    });
                }));
            else
                gapi.client.load('task', apiVersion, function(yy) {
                    console.log("task API loaded. "+JSON.stringify(yy));
                    console.log("task API loaded. "+gapi.client.task);

                    gapi.auth.authorize({client_id: clientId,
                            scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                        function(res)
                        {
                            console.log(res);
                            var getTaskList = gapi.client.task.list();
                            getTaskList.execute(function (data){
                                console.log("task lists = "+JSON.stringify(data));
                                if(data.task_list)
                                    self.updateTaskStore(data.task_list);
                            });
                        });
                }, ROOT);
        },
        updateTaskStore: function(data){
            console.log("updating task store");
            taskStore = new Memory();
            for(var i=0;i<data.length;i++){
                var tmp = new Object();
                tmp.id = data[i].id;
                tmp.title = decodeURI(data[i].title);
                tmp.status = data[i].status;
                tmp.description = decodeURI(data[i].description);
                tmp.local_timestamp = data[i].last_updated;
                tmp.server_timestamp = data[i].last_updated;
                console.log(categoryStore.data);
                /*for(var j=0;j<categoryStore.data.length;j++){
                    if(categoryStore.data[j].name == data[i].category)
                        //tmp.category = data[i].category;
                        tmp.category = categoryStore.data[j].id;
                }*/
                if(typeof(data[i].category)=="undefined")
                    tmp.category = "";
                else
                    tmp.category = data[i].category;
                taskStore.add(tmp);
            }
            console.log(taskStore);
            this.refreshTaskList(localStorage.taskListType);
            this.getContextList();
            this.refreshStatusList();
            this.saveTaskToLocalStorage();
            refreshCategoryList(localStorage.selectedCategoryID_);
        },
        refreshStatusList: function(){
            document.getElementById("status-list").innerHTML = "";
            listWidget = registry.byId("status-list");
            for(var i = 0; i < statuslist.length; i++){
                var taskcount = 0;
                var tmp = taskStore.query({status:statuslist[i]});
                if(tmp.total!=0){
                    taskcount = tmp.total;
                }
                var itemWidget = new dojox.mobile.ListItem({
                    label: statuslist[i],
                    icon:"images/"+iconarray[i],
                    moveTo: "#",
                    rightText: taskcount,
                    onClick:function( e ){moveToTaskListPage(itemWidget,'status',this.label)}
                });
                listWidget.addChild(itemWidget);
            }
        },
        getContextList: function(){
            document.getElementById("home-page-context-list").innerHTML = "";
            var contextlist = [];console.log(taskStore);
            for(var i=0;i<taskStore.data.length;i++){
                var tmp = taskStore.data[i].title;
                var split = tmp.split(" ");
                for(var j=0;j<split.length;j++){
                    if(split[j].indexOf("@")!=-1){
                        var tmp_tmp = split[j].substring(split[j].indexOf("@"),split[j].length);
                        var split_split = tmp_tmp.split("@");
                        var k = 0;
                        for(var ii=0;ii<split_split.length;ii++){
                            for(k=0;k<contextlist.length;k++){
                                if(contextlist[k]=="@"+split_split[ii]){
                                    break;
                                }
                            }
                            if(k==contextlist.length){
                                if(split_split[ii]!="")
                                    contextlist.push("@"+split_split[ii]);
                            }
                        }
                    }
                }

                tmp = taskStore.data[i].description;
                var split = tmp.split(" ");
                for(var j=0;j<split.length;j++){
                    if(split[j].indexOf("@")!=-1){
                        var tmp_tmp = split[j].substring(split[j].indexOf("@"),split[j].length);
                        console.log(tmp_tmp);
                        var split_split = tmp_tmp.split("@");
                        var k = 0;
                        for(var ii=0;ii<split_split.length;ii++){
                            for(k=0;k<contextlist.length;k++){
                                if(contextlist[k]=="@"+split_split[ii]){
                                    break;
                                }
                            }
                            if(k==contextlist.length){
                                if(split_split[ii]!="")
                                    contextlist.push("@"+split_split[ii]);
                            }
                        }
                    }
                }
            }
            console.log(contextlist);

            listWidget = registry.byId("home-page-context-list");
            for(var i_ = 0; i_ < contextlist.length; i_++){
                var taskcount = 0;
                ///get task count
                var taskStoreIndex = -1;
                for(var i=0;i<taskStore.data.length;i++){
                    taskStoreIndex = -1;
                    var tmp = taskStore.data[i].title;
                    var split = tmp.split(" ");
                    for(var j=0;j<split.length;j++){
                        if(split[j].indexOf("@")!=-1){
                            var tmp_tmp = split[j].substring(split[j].indexOf("@"),split[j].length);
                            var split_split = tmp_tmp.split("@");
                            var k = 0;
                            for(var ii=0;ii<split_split.length;ii++){
                                /*for(k=0;k<contextlist.length;k++){
                                 if(contextlist[k]=="@"+split_split[ii]){
                                 taskcount++;
                                 }
                                 }*/
                                /*if(k==contextlist.length){
                                 if(split_split[ii]!="")
                                 taskcount++;
                                 }*/
                                if(contextlist[i_]=="@"+split_split[ii]){
                                    if(taskStoreIndex==-1){
                                        taskStoreIndex=i;
                                        taskcount++;
                                    }
                                }
                            }
                        }
                    }

                    tmp = taskStore.data[i].description;
                    var split = tmp.split(" ");
                    for(var j=0;j<split.length;j++){
                        if(split[j].indexOf("@")!=-1){
                            var tmp_tmp = split[j].substring(split[j].indexOf("@"),split[j].length);
                            var split_split = tmp_tmp.split("@");
                            var k = 0;
                            for(var ii=0;ii<split_split.length;ii++){
                                /*for(k=0;k<contextlist.length;k++){
                                 if(contextlist[k]=="@"+split_split[ii]){
                                 taskcount++;
                                 }
                                 }*/
                                /*if(k==contextlist.length){
                                 if(split_split[ii]!="")
                                 taskcount++;
                                 }*/
                                if(contextlist[i_]=="@"+split_split[ii]){
                                    if(taskStoreIndex==-1){
                                        taskStoreIndex=i;
                                        taskcount++;
                                    }
                                }
                            }
                        }
                    }
                }
                ///
                var itemWidget = new dojox.mobile.ListItem({
                    label: contextlist[i_].substring(1,contextlist[i_].length),
                    icon:"images/drawer.png",
                    moveTo: "#",
                    rightText: taskcount,
                    onClick:function( e ){moveToTaskListPage(itemWidget,'context',this.label)}
                });
                listWidget.addChild(itemWidget);
            }
        }
    };
    return taskUtil;
});