"use strict";

const URL = "http://192.168.1.4:3000";
// const URL = "http://localhost:3000";
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

var stompClient = null;
var userN = 0;
var ids = 0;
var me = null;

var sessionId = null;
var content = null;
var characterIds = [];
var editors;
var viewers;

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

function joinSession(event) {
  event.preventDefault();
  stompClient.send("/app/session/join", {}, JSON.stringify({ sender: me }));
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
        me = { id, username };

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
  const { inputType, data } = event;
  const selectionStart = editor.selectionStart;
  const parentId = selectionStart === 0 ? -1 : characterIds[selectionStart - 1];

  console.log(selectionStart, parentId, data, content);
  console.log(characterIds);
  const selectionEnd = editor.selectionEnd;
  console.log("my current pos:", me.cursorPosition);
  if (inputType === "insertText") {
    stompClient.send(
      `/app/session/${sessionId}/edit`,
      {},
      JSON.stringify({
        sender: me,
        operation: {
          type: "INSERT",
          parentId,
          ch: data,
          userId: 1,
          timestamp: Date.now(),
        },
      })
    );
  } else if (inputType === "deleteContentBackward") {
    stompClient.send(
      `/app/session/${sessionId}/edit`,
      {},
      JSON.stringify({
        sender: me,
        operation: {
          type: "DELETE",
          parentId,
          ch: data,
          userId: 1,
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
  const user = editors.find((editor) => editor.id === me.id);
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
    stompClient.subscribe("/topic/session/" + sessionId, onMessageReceived);
    editors = message.editors;
    console.log(`User: ${me.id} created session: ${sessionId}`);
    me.cursorPosition = 0;
  } else if (message.type === "JOIN") {
    if (message.sender.id === me.id) {
      sessionId = message.sessionId;
      stompClient.subscribe("/topic/session/" + sessionId, onMessageReceived);
      messageElement.classList.add("event-message");
    }

    editors = message.editors;
    viewers = message.viewers;
    console.log(
      `User (${message.sender.id}) joined session (${message.sessionId})`
    );
  } else if (message.type === "UPDATE") {
    updateDocument(message.content, message.characterIds);
    editors = message.editors;
    console.log("Editors", editors);
    updateCursorPosition();
    console.log(`Updating Document...`);
  } else if (message.type === "CURSOR") {
    editors = message.editors;
    updateCursorPosition();
    console.log(`Updating Cursors...`);
  } else if (message.type === "LEAVE") {
    messageElement.classList.add("event-message");
    message.content = message.sender.id + " left!";
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

  const user = editors.find((user) => user.id === me.id);
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
