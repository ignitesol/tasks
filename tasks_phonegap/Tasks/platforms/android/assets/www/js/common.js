var self;

var categoryStore = null;//category store
var taskStore = null;//task store
var contextlist = [];
var statuslist = ["next","waiting","calendar","done","someday"];
var iconarray = ["arrow-right.png","busy.png","calendar.png","checkmark.png","pushpin.png"];
var uncategorized = "uncategorized";
var nostatus = "no status";
var event1 = {};
require([
		"dojo/ready",
		"dijit/registry",
		"dojox/mobile/parser",    // This mobile app uses declarative programming with fast mobile parser
		"dojo/store/Memory",
		"dojo/dom-construct",
        "js/OAuthUtil",
        "js/CategoryUtil",
        "js/TaskUtil",
		"dojox/mobile",           // This is a mobile app.
		"dojox/mobile/TabBar",
		"dojox/mobile/ScrollableView",
		"dojox/mobile/SimpleDialog",
		"dojox/mobile/CheckBox",
		"dojox/mobile/TextBox",
		"dojox/mobile/RoundRectStoreList",
		"dojox/mobile/EdgeToEdgeStoreList",
		"dojox/mobile/TextArea",
		"dojox/mobile/RoundRect",
		"dojox/mobile/FormLayout",
		"dojox/mobile/TextArea",
		"dojox/mobile/RadioButton"
], function(ready,registry,parser,Memory,domConstruct,oAuthUtil,categoryUtil,taskUtil){
    if (device.platform === 'iOS' && parseFloat(device.version) >= 7.0) {
        document.body.style.marginTop = "20px";
    }
	  var flag = true;
	  dojo.subscribe("/dojox/mobile/afterTransitionOut",
		    function(view, moveTo, dir, transition, context, method){
                //alert(moveTo);
		    	var bottom = Math.abs(document.height - localStorage.documentHeight);
		    	document.getElementById('home-page-tab-bar').style.bottom = bottom+'px';
		    	document.getElementById('plusbtn').style.bottom = bottom+'px';
		    	document.getElementById('statusbar').style.bottom = bottom+'px';
		    		//var w = registry.byId('tmp-page');
		  			//w.performTransition(moveTo,1,"",null);
                if(moveTo.indexOf("category-manage-page")!=-1){
                    window.category_changed = true;
                }
		  		if(moveTo.indexOf("add-task-page")!=-1){
                    /*document.getElementById('next').blur();
                    document.getElementById('waiting').blur();
                    document.getElementById('someday').blur();
                    document.getElementById('done').blur();
                    document.getElementById('calendar').blur();*/
                    //setTimeout(function(){registry.byId('title').focus();},1000);
		  		}
                else if(moveTo.indexOf("task-list-page")!=-1){
                    console.log(localStorage.actiontype);
                    if(localStorage.actiontype == "taskInsert"){
                        localStorage.actiontype = "";
                        taskUtil.createTaskToServer();
                    }
                    else if(localStorage.actiontype == "taskUpdate"){
                        console.log("updating");
                        localStorage.actiontype = "";
                        taskUtil.updateTaskToServer();
                    }
                }
                else if(moveTo.indexOf("home-page")!=-1){
                    if(window.category_changed)
                        categoryUtil.synchronize();
                    window.category_changed = false;
                }
	  });
      ready(function(){
          // Wait for device API libraries to load
//
          document.addEventListener("deviceready", onDeviceReady, false);

// device APIs are available
//
          function onDeviceReady() {
              checkConnection();
              setInterval(function(){checkConnection();},5*60*1000);
          }

          function checkConnection() {
              var networkState = navigator.connection.type;

              var states = {};
              states[Connection.UNKNOWN]  = 'Unknown connection';
              states[Connection.ETHERNET] = 'Ethernet connection';
              states[Connection.WIFI]     = 'WiFi connection';
              states[Connection.CELL_2G]  = 'Cell 2G connection';
              states[Connection.CELL_3G]  = 'Cell 3G connection';
              states[Connection.CELL_4G]  = 'Cell 4G connection';
              states[Connection.CELL]     = 'Cell generic connection';
              states[Connection.NONE]     = 'No network connection';

              //alert('Connection type: ' + states[networkState]);
              if(states[networkState] != states[Connection.UNKNOWN] && states[networkState] != states[Connection.NONE]){
                  if(!window.gce_loaded){
                      var element = document.createElement("script");
                      element.setAttribute("src","https://apis.google.com/js/client.js?onload=gce_init");
                      document.getElementsByTagName("head")[0].appendChild(element);
                  }
                  if(oAuthUtil.isAuthorized()){
                      synchronize();
                  }
                  else{
                      oAuthUtil.openAuthWindow();
                  }
              }
          }
        //synchronize();
        /*if(oAuthUtil.isAuthorized())
            //setInterval(categoryUtil.synchronize(),5*60*1000);
            oAuthUtil.synchronize();
        else
            oAuthUtil.openAuthWindow(oAuthUtil.synchronize);*/
        //setInterval(taskUtil.synchronize(),5*60*1000);
        //setTimeout(taskUtil.synchronize(),5*60*000);

      	var backButton = registry.byId("hd-category-manage").backButton;
		if (backButton) {
		  dojo.connect(backButton, "onClick", function() {
		  	localStorage.currentView="home-page";
		  });
		}
		backButton = registry.byId("task-list-page-title").backButton;
		if (backButton) {
		  dojo.connect(backButton, "onClick", function() {
		  	localStorage.currentView="home-page";
		  });
		}
		backButton = registry.byId("add-task-page-title").backButton;
		if (backButton) {
		  dojo.connect(backButton, "onClick", function() {
		  	localStorage.currentView="task-list-page" 
		  });
		}
	  });
    synchronize = function(){
        console.log("synchonizing...");
        categoryUtil.synchronize();
        //setInterval(function(){categoryUtil.synchronize()},5*60*1000);
    };
      onAddCategoryClick = function(){
			localStorage.isModify = false;
			document.getElementById('delete-category-checkbox').style.display = 'none';
			showDlg('category-add-dlg',true);
	  };
	  
	  /***
		init Category Store
	  ***/
	  /*initCategoryStore = function(){
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
	  };*/
	  /***
		init Task Store
	  ***/
	  /*initTaskStore = function(){
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
					taskStore.add(tmp);
				}
			}
	  };*/
	  /***
		refresh category list
		categoryid : if set select category in add task page
	  ***/
	  refreshCategoryList = function(categoryid){
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
				/*var tmp = taskStore.query({category:categoryStore.data[i].id});
				console.log(tmp);
				if(tmp.total!=0){
					taskcount = tmp.total;
				}*/
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
		  };
        /***
         refresh status list
         ***/
        /*refreshStatusList = function(){
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
        };*/
	  /***
		refresh task list
	  ***/
	  /*refreshTaskList = function(task_list_type){
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
	  };
*/
	  /***
		get context 
	  ***/
	  /*getContextList = function(){
		  document.getElementById("home-page-context-list").innerHTML = "";
		  contextlist = [];
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
								  *//*for(k=0;k<contextlist.length;k++){
									  if(contextlist[k]=="@"+split_split[ii]){
										  taskcount++;
									  }
								  }*//*
								  *//*if(k==contextlist.length){
									  if(split_split[ii]!="")
										  taskcount++;
								  }*//*
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
								  *//*for(k=0;k<contextlist.length;k++){
									  if(contextlist[k]=="@"+split_split[ii]){
										  taskcount++;
									  }
								  }*//*
								  *//*if(k==contextlist.length){
									  if(split_split[ii]!="")
										  taskcount++;
								  }*//*
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
	  }*/
	  /***
		  initialize
	  ***/
	  initialize = function(){
          parser.parse();
          
          localStorage.currentView = "home-page";
		  localStorage.selectedCategoryID = '';
		  localStorage.selectedCategoryID_ = '';
		  localStorage.selectedStatus = '';
		  localStorage.selectedStatus_ = '';
		  localStorage.selectedTaskID = '';
		  categoryUtil.initCategoryStore();
		  taskUtil.initTaskStore();
		  categoryUtil.refreshCategoryList();
		  //refreshTaskList();
		  taskUtil.getContextList();
          taskUtil.refreshStatusList();
		  document.getElementById('body').style.display = 'block';
		  
		  var w = registry.byId('tmp-page');
		  w.performTransition('home-page',1,"",null);

	  };
	  localStorage.documentHeight = document.height;
	  //alert(localStorage.documentHeight);
	  initialize();
	  
	  //show dialog
	  showDlg = function(dlg,flag){
		if(dlg=="category-add-dlg" && flag)
			registry.byId("new-category-name").set("value","");
		registry.byId(dlg).show();
	  };
	  /***
		hide dialog
	  ***/
	  hideDlg = function(dlg,flag){
		if(dlg=="category-add-dlg"&&flag){
			
		}
		registry.byId(dlg).hide();
	  };
	  /***
		show category select dialog
	  ***/
	  showSelectCategoryDlg = function(){
		  if(categoryStore.data.length!=0)
			  showDlg("category-select-dlg");
	  };
	  

	  /***
		show Task List Page
		li :
		type : status or category
		id : status string value or category id
	  ***/
	  moveToTaskListPage = function(li,type,id){
	  	  document.getElementById('task-list-page').style.display = 'block';
	  	  localStorage.currentView = "task-list-page";
		  registry.byId(li).transitionTo("task-list-page");
		  if(type=="status"){
			  for(var i=0;i<statuslist.length;i++){
				  registry.byId(statuslist[i]).set('disabled',false);
			  }
			  registry.byId('category').set('moveTo','#');
			  localStorage.selectedStatus = id;
			  localStorage.selectedCategoryID = '';
			  localStorage.selectedContext = "";
			  registry.byId('task-list-page-title').set('label',id);
			  registry.byId('add-task-page-title').set('label',id);
			  registry.byId(id).set('selected',true);
			  localStorage.taskListType = "status";
			  taskUtil.refreshTaskList("status");
		  }
		  else if(type=="category"){
			  for(var i=0;i<statuslist.length;i++){
				  registry.byId(statuslist[i]).set('selected',false);
			  }
			  //registry.byId('category').set('moveTo','');
			  registry.byId('category').set('label','#'+categoryStore.query({ id: id })[0].name);
			  localStorage.selectedStatus = '';
			  localStorage.selectedCategoryID = id;
			  localStorage.selectedContext = "";
			  registry.byId('task-list-page-title').set('label',categoryStore.query({ id: id })[0].name);
			  registry.byId('add-task-page-title').set('label',categoryStore.query({ id: id })[0].name);
			  localStorage.taskListType = "category";
              taskUtil.refreshTaskList("category");
		  }
		  else if(type=="context"){
			  for(var i=0;i<statuslist.length;i++){
				  registry.byId(statuslist[i]).set('selected',false);
			  }
			  registry.byId('category').set('moveTo','#');
			  registry.byId('task-list-page-title').set('label',id);
			  registry.byId('add-task-page-title').set('label',id);
			  localStorage.selectedStatus = "";
			  localStorage.selectedCategoryID = "";
			  localStorage.selectedContext = id;
			  localStorage.taskListType = "context";
              taskUtil.refreshTaskList("context");
		  }
	  };
	  /***
		show Category Edit dialog
		index : category id to edit
	  ***/
	  onCategoryItemClick = function(index){
		registry.byId("new-category-name").set("value",categoryStore.query({ id: index })[0].name);
		localStorage.selectedCategoryID = categoryStore.query({ id: index })[0].id;
		localStorage.isModify = true;
		//document.getElementById('remove-category-checkbox').checked = false;
		document.getElementById('delete-category-checkbox').innerHTML = "<div id='rm-check-div'><input type='checkbox' id='remove-category-checkbox' readonly><label>Delete this Category</label></div> ";
        var box = document.getElementById('delete-category-checkbox');
        box.removeEventListener("touchstart",checkItem);
        box.addEventListener('touchstart', checkItem, false);

        var tmp = taskStore.query({ category:localStorage.selectedCategoryID });
		if(tmp.length!=0)
			document.getElementById('delete-category-checkbox').style.display = 'none';
		else
			document.getElementById('delete-category-checkbox').style.display = 'block';
		showDlg("category-add-dlg");
	  };
	  
	  
	  /***
		select category at add task page
	  ***/
	  onSelectCategory = function(dlg,categoryid){
		  var tmp = categoryStore.query({ id: categoryid });
		  if(tmp.length>0)
			  registry.byId("category").set('label',"#"+categoryStore.query({ id: categoryid })[0].name);
		  else
			  registry.byId("category").set('label',"#"+uncategorized);
		  if(localStorage.taskListType=="category"){
				localStorage.selectedCategoryID_ = categoryid;
                console.log("selected category id = " + localStorage.selectedCategoryID_);
				if(categoryid==''){
					//registry.byId('add-task-page-title').set('label',uncategorized);
                    localStorage.selectedCategoryID_ = localStorage.selectedCategoryID;
                    registry.byId('add-task-page-title').set('label',categoryStore.query({ id: localStorage.selectedCategoryID_ })[0].name);
                    categoryid = localStorage.selectedCategoryID_;
                    registry.byId("category").set('label',"#"+categoryStore.query({ id: categoryid })[0].name);
				}
				else{
					var title = categoryStore.query({ id: categoryid })[0].name;
					registry.byId('add-task-page-title').set('label',title);
					//registry.byId('task-list-page-title').set('label',title);
				}
		  }
		  else{
			  localStorage.selectedCategoryID_ = categoryid;
		  }
		  hideDlg('category-select-dlg');
		  onUpdatingTask(localStorage.selectedStatus_,categoryid);
	  };
	  /***
		select status at add task page
	  ***/
	  onSelectStatus = function(status){
		  if(localStorage.taskListType=="status"){
			/*if(localStorage.isEditTask==true){
				registry.byId('add-task-page-title').set('label',localStorage.selectedStatus);
			}
			else{
				registry.byId('add-task-page-title').set('label',status);
				for (var i=0; i<statuslist.length; i++){
					registry.byId(statuslist[i]).set("selected",false);
				}
				registry.byId(localStorage.selectedStatus).set("selected",true);
			}
			//registry.byId('task-list-page-title').set('label',status);*/
			//registry.byId('add-task-page-title').set('label',localStorage.selectedStatus);
			/*for (var i=0; i<statuslist.length; i++){
				registry.byId(statuslist[i]).set('selected',false);
			}
			setTimeout(function(){registry.byId(localStorage.selectedStatus).set("selected",true)},100);*/
			registry.byId('add-task-page-title').set('label',status);
			onUpdatingTask(status);
		  }
		  else if(localStorage.taskListType=="category"||localStorage.taskListType=="context"){
            //registry.byId('add-task-page-title').set('label',status);
			localStorage.selectedStatus_ = status;
			onUpdatingTask();	
		  }
	  };
	  showAlertDlg = function(title,message){
		  document.getElementById('alert-title_').innderHTML = title;
		  document.getElementById('alert-message').innderHTML = message;
		  showDlg('alert-dlg');
	  }
	  onUpdatingTask = function(status,categoryid){
		  if(localStorage.isEditTask=="true"){
			  if(registry.byId('title').get("value")==""){
					//showDlg('alert-dlg');
                  if(typeof(status)!='undefined')
                      localStorage.selectedStatus_ = status;
                  if(typeof(categoryid)!='undefined')
                      localStorage.selectedCategoryID_ = categoryid;
                  return;
			  }
              taskUtil.updateTask(localStorage.selectedTaskID,status,categoryid);
			  /*taskStore.remove(localStorage.selectedTaskID);
			  if(localStorage.taskListType=="status"){
				  if(typeof(status)=='undefined'||status==''){
					taskStore.add({id:localStorage.selectedTaskID,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus});
				  }
				  else
					taskStore.add({id:localStorage.selectedTaskID,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:status});
			  }
			  else if(localStorage.taskListType=="category"){
				  if(typeof(categoryid)=='undefined')
					  taskStore.add({id:localStorage.selectedTaskID,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID,status:localStorage.selectedStatus_});
				  else
					  taskStore.add({id:localStorage.selectedTaskID,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:categoryid,status:localStorage.selectedStatus_});
			  }
			  else if(localStorage.taskListType=="context"){
				  taskStore.add({id:localStorage.selectedTaskID,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_});
			  }*/
		  }
		  else{
			  if(registry.byId('title').get("value")==""){
				  if(typeof(status)!='undefined')
					  localStorage.selectedStatus_ = status;
				  if(typeof(categoryid)!='undefined')
					  localStorage.selectedCategoryID_ = categoryid;
				  return;
			  }
			  onSaveTaskClick();
		  }
		  //taskUtil.saveTaskToLocalStorage();
	  }
	  /***
		save task when not editing...
	  ***/
	  onSaveTaskClick = function(){
		  /*if(registry.byId('title').get("value")==""){
				
			return;
		  }
		  if(localStorage.isEditTask=="true")
				taskStore.remove(localStorage.selectedTaskID);*/

          /*var id = (new Date()).getTime();
		  if(localStorage.taskListType=="status"){
			  taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_});
		  }
		  else if(localStorage.taskListType=="category"){
			  taskStore.add({id:id,title:registry.byId('title').get('value'),description:registry.byId('description').get('value'),category:localStorage.selectedCategoryID_,status:localStorage.selectedStatus_});
		  }*/
          taskUtil.createTask(registry.byId('title').get('value'),registry.byId('description').get('value'),localStorage.selectedCategoryID_,localStorage.selectedStatus_);


		  localStorage.isEditTask=true;
		  //var w = registry.byId('add-task-page');
		 // w.performTransition('#task-list-page',1,"",null);
	  };
	  /***
		Save Task to local Storage
	  ***/
	  /*saveTaskToLocalStorage = function(){
		  var taskstr = "{tasks:[";
		  for(var i=0;i<taskStore.data.length;i++){
			  taskstr +="{'id':'"+taskStore.data[i].id+"','status':'"+taskStore.data[i].status+"','category':'"+taskStore.data[i].category+"','title':'"+encodeURI(taskStore.data[i].title)+"','description':'"+encodeURI(taskStore.data[i].description)+"'}";
			  if(i!=(taskStore.data.length-1))
				  taskstr += ",";
		  }
		  taskstr += "]}";
		  localStorage.taskData = taskstr;
	  };*/
	  /***
		move to add task page
	  ***/
	  moveToAddTaskPage = function(){
	  	  localStorage.currentView = "add-task-page";
		  if(localStorage.taskListType=="category"){
			  //registry.byId("add-task-page-category").set('style','display:none;');
              registry.byId("add-task-page-category").set('style','display:block;');
		  }
		  else{
			  registry.byId("add-task-page-category").set('style','display:block;');
		  }
		  registry.byId('savetaskbtn').set("style","display:none;");
		  localStorage.isEditTask = false;
		  registry.byId('title').set("value","");
		  registry.byId('description').set("value","");
		  if(localStorage.taskListType=="category"){
			  var tmp = categoryStore.query({id:localStorage.selectedCategoryID});
			  registry.byId('category').set("label",'#'+tmp[0].name);
			  registry.byId('add-task-page-title').set('label',tmp[0].name);
			  localStorage.selectedCategoryID_ = localStorage.selectedCategoryID;
			  localStorage.selectedStatus = "";
			  localStorage.selectedStatus_ = "";
			  for(var i=0;i<statuslist.length;i++)
				  registry.byId(statuslist[i]).set('selected',false);
		  }
		  else{
			  registry.byId('category').set("label","#"+uncategorized);
			  registry.byId('add-task-page-title').set('label',localStorage.selectedStatus);
			  localStorage.selectedCategoryID_ = "";
			  localStorage.selectedStatus_ = localStorage.selectedStatus;
		  }
		  categoryUtil.refreshCategoryList(localStorage.selectedCategoryID_);
		  var w = registry.byId('task-list-page');
		  w.performTransition('#add-task-page',1,"",null);
		  document.getElementById('description').blur();
		  //document.getElementById('title').focus();
	  };
	  /***
		move to edit task page
		taskid : task id to edit
	  ***/
	  moveToEditTaskPage = function(taskid,status){
	      localStorage.currentView = "add-task-page";
		  registry.byId('savetaskbtn').set("style","display:none;");
		  //localStorage.selectedStatus = "";

		  for(var i=0;i<statuslist.length;i++)
				  registry.byId(statuslist[i]).set('selected',false);
		  if(typeof(status)!='undefined'&&status!=''){
			  localStorage.selectedStatus_ = status;
			  registry.byId(localStorage.selectedStatus_).set('selected',true);
		  }
		  
		  localStorage.selectedTaskID = taskid;
		  var task = taskStore.query({ id:taskid });
		  registry.byId('title').set("value",decodeURI(task[0].title));
		  registry.byId('description').set("value",decodeURI(task[0].description));
		  localStorage.selectedCategoryID_ = task[0].category;
		  categoryUtil.refreshCategoryList(task[0].category);
		  var category = categoryStore.query({id:task[0].category});
		  if(category.length>0){
			  registry.byId('category').set("label","#"+category[0].name);
		  }
		  else{
			  registry.byId('category').set("label","#"+uncategorized);
		  }
		  if(localStorage.taskListType=="category"){
			  //registry.byId("add-task-page-category").set('style','display:none;');
		  }
		  else{
			  registry.byId("add-task-page-category").set('style','display:block;');
		  }
		  var w = registry.byId('task-list-page');
		  w.performTransition('#add-task-page',1,"",null);
		  document.getElementById('title').blur();
		  document.getElementById('description').blur();
          localStorage.isEditTask = true;
	  };
	  
	  /***
		on save category click at category manage page
	  ***/
	  onSaveCategoryClick = function(dlg){
			if(registry.byId("new-category-name").get("value")==""){
				return;
			}
			if(localStorage.isModify=="true"){
				/*categoryStore.remove(localStorage.selectedCategoryID);
				if(!document.getElementById('remove-category-checkbox').checked){
					categoryStore.add({id:localStorage.selectedCategoryID,name:registry.byId("new-category-name").get("value")});
				}*/
                if(document.getElementById('remove-category-checkbox').checked)
                    categoryUtil.removeCategory(localStorage.selectedCategoryID);
                else
                    categoryUtil.updateCategory(localStorage.selectedCategoryID,registry.byId("new-category-name").get("value"));
			}
			else{
                //var id = (new Date()).getTime();
				//categoryStore.add({id:id,name:registry.byId("new-category-name").get("value")});
                categoryUtil.createCategory(registry.byId("new-category-name").get("value"));
			}
            //categoryUtil.saveCategoryToLocalStorage();
			//categoryUtil.refreshCategoryList();
			hideDlg(dlg,true);
	  };

	  /***
		Save Category to local Storage
	  ***/
	  /*saveCategoryToLocalStorage = function(){
		  var categorystr = "{categories:[";
		  for(var i=0;i<categoryStore.data.length;i++){
			  categorystr +="{'id':'"+categoryStore.data[i].id+"','name':'"+encodeURI(categoryStore.data[i].name)+"'}";
			  if(i!=(categoryStore.data.length-1))
				  categorystr += ",";
		  }
		  categorystr += "]}";
		  localStorage.categoryData = categorystr;
	  };*/
	  /***
		on cancel click at category manage page
	  ***/
	  onCancelCategoryClick = function(dlg){
		  hideDlg(dlg);
	  };

    openPreviousPage = function(){
        if(localStorage.currentView == "home-page"){
            navigator.app.exitApp();
        }
        else if(localStorage.currentView == "category-manage-page"){
            var w = registry.byId('category-manage-page');
            w.performTransition('#home-page',1,"",null);
            localStorage.currentView = "home-page";
        }
        else if(localStorage.currentView == "add-task-page"){
            var w = registry.byId('add-task-page');
            w.performTransition('#task-list-page',1,"",null);
            localStorage.currentView = "task-list-page";
        }
        else if(localStorage.currentView == "task-list-page"){
            document.getElementById('task-list-page').style.display = 'none';
            var w = registry.byId('category-manage-page');
            w.performTransition('#home-page',1,"",null);
            localStorage.currentView = "home-page";
        }
    };


	  document.addEventListener("backbutton", onBackKeyDown, false);

		function onBackKeyDown() {
			if(localStorage.currentView == "home-page"){
				navigator.app.exitApp();
			}
			else if(localStorage.currentView == "category-manage-page"){
				var w = registry.byId('category-manage-page');
		  		w.performTransition('#home-page',1,"",null);
		  		localStorage.currentView = "home-page";
			}
			else if(localStorage.currentView == "add-task-page"){
				var w = registry.byId('add-task-page');
		  		w.performTransition('#task-list-page',1,"",null);
		  		localStorage.currentView = "task-list-page";
			}
			else if(localStorage.currentView == "task-list-page"){
				document.getElementById('task-list-page').style.display = 'none';
				var w = registry.byId('category-manage-page');
		  		w.performTransition('#home-page',1,"",null);
		  		localStorage.currentView = "home-page";
			}
		}
});
