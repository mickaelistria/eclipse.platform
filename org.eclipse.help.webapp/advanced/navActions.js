/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pierre Candela  - fix for Bug 194911
 *******************************************************************************/

var isIE = navigator.userAgent.indexOf('MSIE') != -1;

function toggleAutosynch(button) {
    var tocFrame = window.parent.tocViewFrame;
    tocFrame.toggleAutosynch();
    if (tocFrame.isAutosynchEnabled()) {
        try {
		    parent.parent.parent.parent.ContentFrame.ContentToolbarFrame.autosynch();
	    } catch(e){
	    }
    }
    if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function toggleShowAll(button){
	window.parent.parent.toggleShowAll();
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function toggleShowCategories(button){
	parent.searchViewFrame.toggleShowCategories();
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function toggleShowDescriptions(button){
	parent.searchViewFrame.toggleShowDescriptions();
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function removeBookmark(button){
	try {
		parent.bookmarksViewFrame.removeBookmark();
	} catch(e){
	}
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function removeAllBookmarks(button){
	try {
		parent.bookmarksViewFrame.removeAllBookmarks();
	} catch(e){
	}
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function printTopic(errorMsg) {
	var href = parent.tocViewFrame.getSelectedTopic();
	if (href) {
	    parent.parent.parent.parent.ContentFrame.ContentToolbarFrame.printContent();
	}
	else {
		alert(errorMsg);
	}
}

function printToc(errorMsg) {
	var topic = parent.tocViewFrame.getSelectedTopic();
	if (topic && topic != ":blank") {
		var contentRect = getWindowBounds(parent.parent.parent.parent.ContentFrame.ContentViewFrame.window);
		var topRect = getWindowBounds(parent.parent.parent.parent.parent);
		var w = contentRect.width;
		var h = topRect.height;
		var x = topRect.x + (topRect.width - w)/2;
		var y = topRect.y;
		var indexAnchor=topic.indexOf('#');
	    var parameters;			
	    if (indexAnchor!=-1) {
		    var anchor=topic.substr(indexAnchor+1);
		    topic=topic.substr(0,indexAnchor);
		    parameters = "?topic="+topic+"&anchor="+anchor;	
	     } else {
		    parameters = "?topic="+topic;
	     }

		var printWindow = window.open("print.jsp" + parameters, "printWindow", "directories=yes,location=no,menubar=yes,resizable=yes,scrollbars=yes,status=yes,titlebar=yes,toolbar=yes,width=" + w + ",height=" + h + ",left=" + x + ",top=" + y);
		printWindow.focus();
	}
	else {
		alert(errorMsg);
	}
}

function collapseAll(button) {
    try {
		parent.tocViewFrame.collapseAll();
	} catch(e){
	}
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}

function getWindowBounds(window) {
	var rect = new Object();
	if (window.screenLeft) {
		rect.x = window.screenLeft;
		rect.y = window.screenTop;
	}
	else {
		rect.x = window.screenX;
		rect.y = window.screenY;
	}

	if (window.innerWidth) {
		rect.width = window.innerWidth;
		rect.height = window.innerHeight;
	}
	else {
		rect.width = window.document.body.clientWidth;
		rect.height = window.document.body.clientHeight;
	}
	return rect;
}
