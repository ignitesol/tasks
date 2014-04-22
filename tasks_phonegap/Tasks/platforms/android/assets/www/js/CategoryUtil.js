/**
 * Created by Leonid on 1/17/14.
 */

define([
    "js/OAuthUtil",
    "dojo/store/Memory",
    "dijit/registry",
    "js/TaskUtil"
], function (oAuthUtil,Memory,registry,taskUtil)
{
    var categoryUtil = {
        synchronize: function(){
            localStorage.args = "";
            localStorage.actiontype = "";
        	var self = this;
            if(window.gce_loaded)
                this.upload(0);
            else{
                setTimeout(function(){self.synchronize()},10000);
            }
        },
        initCategoryStore: function(){
            categoryStore = new Memory();
            var jsObj = eval(localStorage.categoryData);
            console.log(jsObj);
            //var arr = jsObj.categories;
            //console.log(arr);
            if(typeof(jsObj)!='undefined'){
                for(var i=0;i<jsObj.length;i++){
                    var tmp = new Object();
                    tmp.id = jsObj[i].id;
                    tmp.name = decodeURI(jsObj[i].name);
                    tmp.local_timestamp = jsObj[i].local_timestamp;
                    tmp.server_timestamp = jsObj[i].server_timestamp;
                    categoryStore.add(tmp);
                }
            }
        },
        refreshCategoryList: function(categoryid){
            //refresh add task page's category list
            document.getElementById("category-select-list").innerHTML = "";
            var str =""
            for(var i = 0; i < categoryStore.data.length; i++){
                if(categoryid==categoryStore.data[i].id)
                    str += '<div class="row" onclick="onSelectCategory(\'category-select-dlg\',\''+categoryStore.data[i].id+'\');this.childNodes[0].checked=true;"><input name="group" type="radio" data-dojo-type="dojox.mobile.RadioButton" style="float:left;" onchange="if(this.checked){ onSelectCategory(\'category-select-dlg\',\''+categoryStore.data[i].id+'\');}" checked></input><label style="float:left;">'+categoryStore.data[i].name+'</label></div>';
                else
                    str += '<div class="row" onclick="onSelectCategory(\'category-select-dlg\',\''+categoryStore.data[i].id+'\');this.childNodes[0].checked=true;"><input name="group" type="radio" data-dojo-type="dojox.mobile.RadioButton" style="float:left;" onchange="if(this.checked){ onSelectCategory(\'category-select-dlg\',\''+categoryStore.data[i].id+'\');}"></input><label style="float:left;">'+categoryStore.data[i].name+'</label></div>';
            }
            if(typeof(categoryid)=='undefined'||categoryid==''){
                str += '<div class="row" onclick="onSelectCategory(\'category-select-dlg\',\'\');this.childNodes[0].checked=true;"><input name="group" type="radio" data-dojo-type="dojox.mobile.RadioButton" style="float:left;" onchange="if(this.checked){ onSelectCategory(\'category-select-dlg\',\'\');}" checked></input><label style="float:left;">'+uncategorized+'</label></div>';
            }
            else
                str += '<div class="row" onclick="onSelectCategory(\'category-select-dlg\',\'\');this.childNodes[0].checked=true;"><input name="group" type="radio" data-dojo-type="dojox.mobile.RadioButton" style="float:left;" onchange="if(this.checked){ onSelectCategory(\'category-select-dlg\',\'\');}"></input><label style="float:left;">'+uncategorized+'</label></div>';
            document.getElementById("category-select-list").innerHTML = str;

            var listWidget = registry.byId("category-list");

            //refresh manage category page's category list
            document.getElementById("category-list").innerHTML = "";
            //add list item from store
            for(var i = 0; i < categoryStore.data.length; i++){
                var itemWidget = new dojox.mobile.ListItem({
                    label: categoryStore.data[i].name,
                    index: categoryStore.data[i].id,
                    icon:'images/tags.png',
                    moveTo: "#",
                    onClick:function( e ){onCategoryItemClick(this.index)}
                });
                listWidget.addChild(itemWidget);
            }
            //add add new category item
            var itemWidget = new dojox.mobile.ListItem({
                label: "Add New Category",
                onClick:onAddCategoryClick,
                moveTo: "#"
            });
            listWidget.addChild(itemWidget);

            //refresh home page's category list
            document.getElementById("home-page-category-list").innerHTML = "";
            listWidget = registry.byId("home-page-category-list");
            for(var i = 0; i < categoryStore.data.length; i++){
                var id = categoryStore.data[i].id;
                var taskcount = 0;
                var tmp = taskStore.query({category:categoryStore.data[i].id}).forEach(function(item){
                    // called for each match
                    if(item.status != "done"){
                        taskcount ++;
                    }
                });
                console.log(tmp);

                var itemWidget = new dojox.mobile.ListItem({
                    label: categoryStore.data[i].name,
                    index: categoryStore.data[i].id,
                    icon:'images/book.png',
                    moveTo: "#",
                    rightText: taskcount,
                    onClick:function( e ){moveToTaskListPage(itemWidget,'category',this.index)}
                });
                listWidget.addChild(itemWidget);
            }
        },
        saveCategoryToLocalStorage: function(){
            var categorystr = "{categories:[";
            for(var i=0;i<categoryStore.data.length;i++){
                categorystr +="{'id':'"+categoryStore.data[i].id+"','local_timestamp':'"+categoryStore.data[i].local_timestamp+"','server_timestamp':'"+categoryStore.data[i].server_timestamp+"','name':'"+encodeURI(categoryStore.data[i].name)+"'}";
                if(i!=(categoryStore.data.length-1))
                    categorystr += ",";
            }
            categorystr += "]}";
            localStorage.categoryData = categorystr;
        },
        updateCategoryStore: function(categoryObjArr){
            console.log("updating category store");
            categoryStore = new Memory();
            for(var i=0;i<categoryObjArr.length;i++){
                var tmp = new Object();
                tmp.id = categoryObjArr[i].id;
                tmp.name = decodeURI(categoryObjArr[i].name);
                tmp.local_timestamp = categoryObjArr[i].last_updated;
                tmp.server_timestamp = categoryObjArr[i].last_updated;
                categoryStore.add(tmp);
            }
            this.refreshCategoryList();
            this.saveCategoryToLocalStorage();
            taskUtil.synchronize();
        },
        upload: function(index){
            var self = this;
            if(index>=categoryStore.data.length){
                self.read();
                return;
            }
            if(categoryStore.data[index].local_timestamp!=categoryStore.data[index].server_timestamp){//if changed category
                if(typeof(categoryStore.data[index].server_timestamp)=="undefined"){//if new category
                    if(islocal)
                        oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                            var arg = {name:categoryStore.data[index].name};
                            var insertCategory = gapi.client.task.category.insert(arg);
                            insertCategory.execute(function (data){
                                console.log(JSON.stringify(data));
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
                                    var arg = {name:categoryStore.data[index].name};
                                    var insertCategory = gapi.client.task.category.insert(arg);
                                    insertCategory.execute(function (data){
                                        console.log(JSON.stringify(data));
                                        index++;
                                        self.upload(index);
                                    });
                                });
                        }, ROOT);
                }
                else{//if category update
                    if(islocal)
                        oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                            var arg = {new_name:categoryStore.data[index].name,client_copy_timestamp:categoryStore.data[index].server_timestamp,id:categoryStore.data[index].id};
                            var updateCategory = gapi.client.task.category.update(arg);
                            updateCategory.execute(function (data){
                                console.log(JSON.stringify(data));
                                if(data.id){
                                    categoryStore.remove(categoryStore.data[index].id);
                                    categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                                    self.saveCategoryToLocalStorage();
                                    self.refreshCategoryList();
                                    //update task store and refresh task store
                                }
                            });
                        }));
                    else
                        gapi.client.load('task', apiVersion, function(yy) {
                            gapi.auth.authorize({client_id: clientId,
                                    scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                                function(res)
                                {
                                    console.log(res);
                                    var arg = {new_name:categoryStore.data[index].name,client_copy_timestamp:categoryStore.data[index].server_timestamp,id:categoryStore.data[index].id};
                                    var updateCategory = gapi.client.task.category.update(arg);
                                    updateCategory.execute(function (data){
                                        console.log(JSON.stringify(data));
                                        if(data.id){
                                            categoryStore.remove(categoryStore.data[index].id);
                                            categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                                            self.saveCategoryToLocalStorage();
                                            self.refreshCategoryList();
                                            //update task store and refresh task store
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
        createCategory: function(name){
            var id = (new Date()).getTime();
            categoryStore.add({id:id,name:name,local_timestamp:id});
            var self = this;
            self.saveCategoryToLocalStorage();
            self.refreshCategoryList();
            /*if(islocal)
                oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                    var arg = {name:name};
                    var insertCategory = gapi.client.task.category.insert(arg);
                    insertCategory.execute(function (data){
                        console.log(JSON.stringify(data));
                        if(data.id){
                            categoryStore.remove(id);
                            categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                        }
                        self.saveCategoryToLocalStorage();
                        self.refreshCategoryList();
                        //update task store and refresh task store
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
                            var arg = {name:name};
                            var insertCategory = gapi.client.task.category.insert(arg);
                            insertCategory.execute(function (data){
                                console.log(JSON.stringify(data));
                                if(data.id){
                                    categoryStore.remove(id);
                                    categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                                }
                                self.saveCategoryToLocalStorage();
                                self.refreshCategoryList();
                                //update task store and refresh task store
                            });
                        });
                }, ROOT);*/
        },
        update: function(){

        },
        delete: function(){

        },
        removeCategory: function(id){
            var self = this;
            var category = categoryStore.get(id);
            categoryStore.remove(id);
            self.saveCategoryToLocalStorage();
            self.refreshCategoryList();

            if(islocal)
                oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                    var arg = {client_copy_timestamp:category.server_timestamp,id:id};
                    console.log(arg);
                    var removeCategory = gapi.client.task.category.delete(arg);
                    removeCategory.execute(function (data){
                        console.log("category lists = "+JSON.stringify(data));
                    });
                }));
            else
                gapi.client.load('task', apiVersion, function(yy) {
                    gapi.auth.authorize({client_id: clientId,
                            scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                        function(res)
                        {
                            console.log(res);
                            var arg = {client_copy_timestamp:category.local_timestamp,id:id};
                            console.log(arg);
                            var removeCategory = gapi.client.task.category.delete(arg);
                            removeCategory.execute(function (data){
                                console.log("category lists = "+JSON.stringify(data));
                            });
                        });
                }, ROOT);
        },
        updateCategory: function(id,name){
            var self = this;
            var category = categoryStore.get(id);
            console.log(category);
            /*if(category.local_timestamp!=category.server_timestamp){
                categoryStore.remove(id);
                this.createCategory(name);
            }
            else{*/
                var server_timestamp = category.server_timestamp;
                categoryStore.remove(id);
                var newid = (new Date()).getTime();
                categoryStore.add({id:id,name:name,local_timestamp:newid,server_timestamp:server_timestamp});
                self.saveCategoryToLocalStorage();
                self.refreshCategoryList();
                /*if(islocal)
                    oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                        var arg = {new_name:name,client_copy_timestamp:server_timestamp,id:category.id};
                        var updateCategory = gapi.client.task.category.update(arg);
                        updateCategory.execute(function (data){
                            console.log(JSON.stringify(data));
                            if(data.id){
                                categoryStore.remove(id);
                                categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                                self.saveCategoryToLocalStorage();
                                self.refreshCategoryList();
                                //update task store and refresh task store
                            }
                        });
                    }));
                else
                    gapi.client.load('task', apiVersion, function(yy) {
                        gapi.auth.authorize({client_id: clientId,
                                scope: 'https://www.googleapis.com/auth/userinfo.email', immediate: false},
                            function(res)
                            {
                                console.log(res);
                                var arg = {new_name:name,client_copy_timestamp:server_timestamp,id:category.id};
                                var updateCategory = gapi.client.task.category.update(arg);
                                updateCategory.execute(function (data){
                                    console.log(JSON.stringify(data));
                                    if(data.id){
                                        categoryStore.remove(id);
                                        categoryStore.add({id:data.id,name:data.name,local_timestamp:data.last_updated,server_timestamp:data.last_updated});
                                        self.saveCategoryToLocalStorage();
                                        self.refreshCategoryList();
                                        //update task store and refresh task store
                                    }
                                });
                            });
                    }, ROOT);*/
            //}
        },
        read:function(){
            var self = this;
            if(islocal)
                oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                    var getCategoryList = gapi.client.task.category.list();
                    getCategoryList.execute(function (data){
                        console.log("category lists = "+JSON.stringify(data));
                        if(data.category_list)
                            self.updateCategoryStore(data.category_list);
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
                            var getCategoryList = gapi.client.task.category.list();
                            getCategoryList.execute(function (data){
                                console.log("category lists = "+JSON.stringify(data));
                                if(data.category_list)
                                    self.updateCategoryStore(data.category_list);
                            });
                        });
                }, ROOT);
        }
    };
    return categoryUtil;
});