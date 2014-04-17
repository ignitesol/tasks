/**
 * Created by Leonid on 1/17/14.
 */

window.gce_loaded = false;//gce loaded or not
window.online = false;
window.category_changed = false;
window.touch_event_binded = false;
var apiVersion = "v1";
var clientId = "572251750016-qg6jih41a4t765lkakujjh97gf7hdiec.apps.googleusercontent.com";
var ROOT = 'https://usersource-tasks-test.appspot.com/_ah/api';
var islocal = false;
if(location.href.indexOf("http://")==-1){
    islocal = true;
}

function gce_init(){
    console.log("gce loaded");
    window.gce_loaded = true;
}

function checkItem(e){
    e.preventDefault();
    if(document.getElementById("remove-category-checkbox").checked)
        document.getElementById("remove-category-checkbox").checked=false;
    else
        document.getElementById("remove-category-checkbox").checked=true;
}
