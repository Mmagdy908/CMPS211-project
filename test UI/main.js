"use strict";

//const URL = "http://192.168.1.4:3000";
const URL = "http://localhost:3000";
var usernamePage = document.querySelector("#username-page");
var chatPage = document.querySelector("#doc-page");
var createBtn = document.querySelector(".create");
var joinBtn = document.querySelector(".join-code-submit");
var usernameForm = document.querySelector("#username-form");
var codeForm = document.querySelector("#code-form");
// var messageForm = document.querySelector("#messageForm");
var messageInput = document.querySelector("#message");
var usernameInput = document.querySelector("#name");
var messageArea = document.querySelector("#editor");
var connectingElement = document.querySelector(".connecting");
const editor = document.getElementById("editor");
const log = document.getElementById("log");

let activeUsers = []; // [{ userId, name, color }]
const usersUl = document.getElementById("users");
const editorCodeSpan = document.getElementById("editor-code");
const viewerCodeSpan = document.getElementById("viewer-code");
const undoButton = document.getElementById("undo-button");
const redoButton = document.getElementById("redo-button");

var stompClient = null;
var userN = 0;
var ids = 0;
var me = null;

var sessionId = null;
var content = null;
var characterIds = [];
var editors;
var viewers;
let charIds = 0;

var colors = [
  "#2196F3",
  "#32c787",
  "#00BCD4",
  "#ff5652",
  "#ffc107",
  "#ff85af",
  "#FF9800",
  "#39bbb0",
];

function connect(event) {
  if (event) event.preventDefault();

  //   code = document.querySelector("#code").value.trim();

  if (me?.username) {
    // usernamePage.classList.add("hidden");
    // chatPage.classList.remove("hidden");

    var socket = new SockJS(`${URL}/ws`);
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
  }
}

function onConnected() {
  // Subscribe to the Public Topic
  stompClient.subscribe("/topic/user/" + me.id, onMessageReceived);
  // Tell your username to the server
  stompClient.send(
    "/app/session/1",
    {},
    JSON.stringify({ sender: me.username, type: "REGISTER" })
  );

  connectingElement.classList.add("hidden");
}

function createSession(event) {
  event.preventDefault();
  stompClient.send("/app/session/create", {}, JSON.stringify(me));
  usernamePage.classList.add("hidden");
  chatPage.classList.remove("hidden");
}

// function joinSession(event) {
//   event.preventDefault();
//   stompClient.send(
//     "/app/session/join",
//     {},
//     JSON.stringify({ senderId: userId })
//   );
//   usernamePage.classList.add("hidden");
//   chatPage.classList.remove("hidden");
// }

function joinSession(event) {
  event.preventDefault();

  const code = document.querySelector("#code").value.trim();
  if (!code) {
    alert("Please enter an editor‑ or viewer‑code");
    return;
  }

  stompClient.send(
    "/app/session/join",
    {},
    JSON.stringify({
      sender: me,
      code: code,

    })
  );

  usernamePage.classList.add("hidden");
  chatPage.classList.remove("hidden");
}

function onError(error) {
  connectingElement.textContent =
    "Could not connect to WebSocket server. Please refresh this page to try again!";
  connectingElement.style.color = "red";
}

function createUser(event) {
  event.preventDefault();
  const username = usernameInput.value?.trim();
  if (username) {
    fetch(`${URL}/users`, {
      method: "POST",
      headers: {
        "Content-Type": "text/plain", // Use 'text/plain' for sending plain text
      },
      body: username,
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Network response was not ok " + response.statusText);
        }
        return response.text();
      })
      .then((id) => {
        me = { id: +id, username };

        console.log(`new user id: ${id}`);
        connect();
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }
}
function sendMessage(event) {
  event.preventDefault();
  var messageContent = messageInput.value.trim();
  if (messageContent && stompClient) {
    var chatMessage = {
      sender: username,
      content: messageInput.value,
      type: "CHAT",
    };
    stompClient.send("/app/session/1", {}, JSON.stringify(chatMessage));
    messageInput.value = "";
  }
}
function updateDocument(newContent, newCharacterIds) {
  // const selectionStart = editor.selectionStart;
  // const selectionEnd = editor.selectionEnd;

  content = editor.value = newContent;
  characterIds = newCharacterIds;
}

function editDocument(event) {
  let { inputType, data } = event;
  const selectionStart = editor.selectionStart;
  const parentId = selectionStart === 0 ? -1 : characterIds[selectionStart - 1];
  // const characterId = `${me.id}:${charIds++}`;

  console.log(selectionStart, parentId, data, content);
  console.log(characterIds);
  const selectionEnd = editor.selectionEnd;
  console.log("my current pos:", me.cursorPosition);
  if (inputType === "insertText" || inputType === "insertLineBreak") {
    const characterId = `${me.id}:${charIds++}`;
    characterIds.splice(selectionStart, 0, characterId);
    me.cursorPosition++;

    if (inputType === "insertLineBreak") data = "\n";

    stompClient.send(
      `/app/session/${sessionId}/edit`,
      {},
      JSON.stringify({
        sender: me,
        operation: {
          type: "INSERT",
          parentId,
          ch: data,
          userId: +me.id,
          characterId,
          timestamp: Date.now(),
        },
      })
    );
  } else if (inputType === "insertFromPaste") {
    me.cursorPosition += data.length;
    const characterIdList = data.split("").map((_) => `${me.id}:${charIds++}`);
    characterIds.splice(selectionStart, 0, ...characterIdList);
    stompClient.send(
      `/app/session/${sessionId}/edit`,
      {},
      JSON.stringify({
        sender: me,
        operation: {
          type: "INSERT",
          parentId,
          userId: +me.id,
          timestamp: Date.now(),
        },
        text: data,
        characterIdList,
      })
    );
  } else if (inputType === "deleteContentBackward") {
    if (selectionStart === 0) return;
    me.cursorPosition--;
    characterIds.splice(selectionStart - 1, 1);

    stompClient.send(
      `/app/session/${sessionId}/edit`,
      {},
      JSON.stringify({
        sender: me,
        operation: {
          type: "DELETE",
          parentId,
          ch: data,
          userId: +me.id,
          timestamp: Date.now(),
        },
      })
    );
  }
  // console.log(selectionStart, selectionEnd);
  // console.log(editor.value);
  // if (editor.value === "aaa") editor.value = "YEAH";
}

function updateCursorPosition() {
  const user = editors.find((editor) => editor.id == me.id);
  if (user.cursorPosition !== editor.selectionStart) {
    me.cursorPosition = user.cursorPosition;
    editor.selectionStart = editor.selectionEnd = user.cursorPosition;
  }

  console.log(editors);
}

function onMessageReceived(payload) {
  var message = JSON.parse(payload.body);

  var messageElement = document.createElement("li");
  

  if (message.type == "CREATE") {
    sessionId = message.sessionId;

    if (message.editorCode) {
      editorCodeSpan.textContent = message.editorCode;
      viewerCodeSpan.textContent = message.viewerCode;
    }

    stompClient.subscribe("/topic/session/" + sessionId, onMessageReceived);
    editors = message.editors;
    console.log(`User: ${me.id} created session: ${sessionId}`);
    me.cursorPosition = 0;
  } else if (message.type === "JOIN") {
    console.log(`sender ${message.sender.id}, me:${me.id}`);

    if (message.sender.id === me.id) {
      sessionId = message.sessionId;

      if (!message.isEditor) {
        editor.setAttribute("disabled", "disabled");
      }

      stompClient.subscribe("/topic/session/" + sessionId, onMessageReceived);
      messageElement.classList.add("event-message");

      addUser(message.senderId);
    }

    editors = message.editors;
    viewers = message.viewers;
    console.log(
      `User (${message.sender.id}) joined session (${message.sessionId})`
    );
  } else if (message.type === "CURSOR") {
    // removeUser(message.senderId);
    // showRemoteCursor(message.cursor);
    editors = message.editors;
    updateCursorPosition();
    console.log(`Updating Cursors...`);
  } else if (message.type === "UPDATE") {
    updateDocument(message.content, message.characterIds);
    editors = message.editors;
    if (message.sender.id !== me.id) updateCursorPosition();
    console.log("Editors", editors);
    // updateCursorPosition();
    // console.log(`Updating Document...`);
    // } else if (message.type === "CURSOR") {
    // editors = message.editors;
    // updateCursorPosition();
    // console.log(`Updating Cursors...`);
  } else if (message.type === "LEAVE") {
    // messageElement.classList.add("event-message");
    // message.content = message.sender.id + " left!";
    removeUser(message.senderId);
  } else if (message.type === "PRESENCE") {
    setActiveUsers(message.activeUsers);

    console.log(`Updating Document...`);
  } else {
    // messageElement.classList.add("chat-message");
    // var avatarElement = document.createElement("i");
    // var avatarText = document.createTextNode(message.sender[0]);
    // avatarElement.appendChild(avatarText);
    // avatarElement.style["background-color"] = getAvatarColor(message.sender);
    // messageElement.appendChild(avatarElement);
    // var usernameElement = document.createElement("span");
    // var usernameText = document.createTextNode(message.sender);
    // usernameElement.appendChild(usernameText);
    // messageElement.appendChild(usernameElement);
  }

  var textElement = document.createElement("p");
  var messageText = document.createTextNode(message.content);
  textElement.appendChild(messageText);

  messageElement.appendChild(textElement);

  messageArea.appendChild(messageElement);
  messageArea.scrollTop = messageArea.scrollHeight;
}

// function setActiveUsers(list) {
//   activeUsers = list.map((u,i) => ({
//     userId: u.userId,
//     name: u.name,
//     color: colors[i % colors.length]
//   }));
//   renderUserList();
// }

function setActiveUsers(listOfUserIds) {
  // listOfUserIds is e.g. ["u1","u2","u3"]
  activeUsers = listOfUserIds.map((uid, i) => ({
    userId: uid,
    name: uid, // display the id as the name
    color: colors[i % colors.length],
  }));
  renderUserList();
}

function addUser(userId) {
  if (!activeUsers.find((u) => u.userId === userId)) {
    activeUsers.push({
      userId,
      name: userId,
      color: colors[activeUsers.length % colors.length],
    });
    renderUserList();
  }
}

function removeUser(userId) {
  activeUsers = activeUsers.filter((u) => u.userId !== userId);
  messageElement.classList.add("event-message");
  message.content = message.sender + " left!";
  renderUserList();
}

// function renderUserList() {
//   usersUl.innerHTML = "";
//   activeUsers.forEach(u => {
//     const li = document.createElement("li");
//     li.textContent = u.id;//u.name;
//     li.style.color = u.color;
//     usersUl.appendChild(li);
//   });
// }

function renderUserList() {
  usersUl.innerHTML = "";
  activeUsers.forEach((u) => {
    const li = document.createElement("li");
    // display the username rather than raw id
    li.textContent = u.name;
    li.style.color = u.color;
    usersUl.appendChild(li);
  });
}

// whenever caret moves, broadcast it:
// editor.addEventListener("keyup", sendCursor);
// editor.addEventListener("mouseup", sendCursor);

// function sendCursor() {
//   if (!sessionId) return;
//   const pos = editor.selectionStart;
//   stompClient.send(
//     `/app/session/${sessionId}/edit`,
//     {},
//     JSON.stringify({
//       senderId: userId,
//       type: "CURSOR",
//       cursor: { position: pos },
//     })
//   );
// }

// show remote cursor by overlaying a marker (simple version updates user‑list)
// function showRemoteCursor({ userId: uid, position }) {
//   // highlight that user in the list with their position
//   const li = Array.from(usersUl.children).find((li) => li.textContent === uid);
//   if (li) li.textContent = `${uid} @${position}`;
// }

undoButton.addEventListener("click", () => {
  if (!sessionId || !stompClient) {
    console.log("No active session or connection");
    return;
  }

  stompClient.send(
    `/app/session/${sessionId}/undo`,
    {},
    JSON.stringify({
      sender: me,
      type: "UNDO"  // This matches the enum value we just added
    })
  );

  console.log("Sent undo request");
});

redoButton.addEventListener("click", () => {
  if (!sessionId || !stompClient) {
    console.log("No active session or connection");
    return;
  }

  stompClient.send(
    `/app/session/${sessionId}/redo`,
    {},
    JSON.stringify({
      sender: me,
      type: "REDO"  // This matches the enum value we just added
    })
  );

  console.log("Sent redo request");
});

function getAvatarColor(messageSender) {
  var hash = 0;
  for (var i = 0; i < messageSender.length; i++) {
    hash = 31 * hash + messageSender.charCodeAt(i);
  }
  var index = Math.abs(hash % colors.length);
  return colors[index];
}

createBtn.addEventListener("click", createSession, true);
joinBtn.addEventListener("click", joinSession, true);
codeForm.addEventListener("submit", connect, true);
// messageForm.addEventListener("submit", sendMessage, true);
usernameForm.addEventListener("submit", createUser, true);
editor.addEventListener("beforeinput", editDocument);
editor.addEventListener("keyup", editCursorPosition);
editor.addEventListener("mouseup", editCursorPosition);
editor.addEventListener("focus", editCursorPosition);
// editor.addEventListener("input", editCursorPosition);

function editCursorPosition(event) {
  if (
    event.type === "keyup" &&
    !["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.code)
  ) {
    return;
  }

  const start = editor.selectionStart;
  const end = editor.selectionEnd;

  // console.log(editors);
  const user = editors.find((user) => user.id == me.id);
  if (user) {
    me.cursorPosition = user.cursorPosition = start;
    stompClient.send(
      `/app/session/${sessionId}/update-cursor`,
      {},
      JSON.stringify({
        sender: me,
        type: "CURSOR",
        editors,
      })
    );
  }
}
// editor.addEventListener("beforeinput", (e) => {
//   const { inputType, data } = e;
//   const selectionStart = editor.selectionStart;
//   const selectionEnd = editor.selectionEnd;

//   console.log(`you inserted: ${data}`);
//   // console.log(selectionStart, selectionEnd);
//   // console.log(editor.value);
//   // if (editor.value === "aaa") editor.value = "YEAH";
// });

// editor.addEventListener("input", (e) => {
//   const { inputType, data } = e;
//   const selectionStart = editor.selectionStart;
//   const selectionEnd = editor.selectionEnd;

//   console.log(selectionStart, selectionEnd);
//   console.log(editor.value);
//   if (editor.value === "aaa") editor.selectionStart = editor.selectionEnd = 1;
// });

// Import/Export handlers
const importButton = document.getElementById("import-button");
const importFile = document.getElementById("import-file");
const exportButton = document.getElementById("export-button");

// Update the paste handler
if (importButton && importFile) {
  importButton.addEventListener("click", () => importFile.click());
  importFile.addEventListener("change", (event) => {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function (e) {
      const text = e.target.result;
      // clear local editor
      editor.value = "";

      // Instead of sending each character as an individual operation,
      // send the entire text as one operation
      const parentId = -1;
      const characterIdList = text.split("").map((_) => `${me.id}:${charIds++}`);

      stompClient.send(
        `/app/session/${sessionId}/edit`,
        {},
        JSON.stringify({
          sender: me,
          operation: {
            type: "INSERT",
            parentId: parentId,
            userId: me.id,
            timestamp: Date.now(),
          },
          text: text,
          characterIdList: characterIdList
        })
      );

      // Update local editor right away for responsiveness
      editor.value = text;

      // Update our character ID array 
      characterIds = [...characterIdList];

      // Update cursor position
      me.cursorPosition = text.length;
    };
    reader.readAsText(file);
  });
}

if (exportButton) {
  exportButton.addEventListener("click", () => {
    // use the latest collaborative content variable
    const text = content != null ? content : editor.value;
    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "document.txt";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  });
}
