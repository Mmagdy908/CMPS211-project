 package com.apt.project.collaborative_text_editor.event;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.context.event.EventListener;
 import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
 import org.springframework.stereotype.Component;
 import org.springframework.web.socket.messaging.SessionConnectedEvent;
 import org.springframework.web.socket.messaging.SessionDisconnectEvent;
 import java.util.UUID;

// @Component
// @Slf4j
// @RequiredArgsConstructor
// public class WebSocketEventListener {

//     private final SimpMessageSendingOperations messagingTemplate;
//     public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
//         this.messagingTemplate = messagingTemplate;
//     }
//     @EventListener
//     public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//         String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
//         String uniqueId = UUID.randomUUID().toString();

//         // messagingTemplate.convertAndSendToUser(
//         //     sessionId, // Target the specific session
//         //     "/user/queue/unique-id", // Destination queue for the client
//         //     sessionId // Payload (the unique ID)
//         // );
//         System.out.println("Received a new web socket connection");
//         System.out.println(sessionId);
//     }
// }
// import java.util.concurrent.ConcurrentHashMap;
// import org.springframework.context.event.EventListener;
// import org.springframework.stereotype.Component;
// import org.springframework.web.socket.messaging.SessionConnectEvent;

//     @Component
//     public class WebSocketEventListener {
//         private final ConcurrentHashMap<String, String> activeUsers = new ConcurrentHashMap<>();

//         public WebSocketEventListener(){
//             System.out.println("Beeen");
//         }
//         @EventListener
//         public void handleWebSocketConnect(SessionConnectEvent event) {
//             String userId = event.getMessage().getHeaders().get("simpSessionId").toString();
//             activeUsers.put(userId, userId);
//             System.out.println("Beeen");

//             System.out.println("Client connected. User ID: " + userId + ", Active users: " + activeUsers.size());
//         }
//     }

// import org.springframework.web.socket.handler.TextWebSocketHandler;
// import org.springframework.web.socket.WebSocketSession;
// import org.springframework.web.socket.CloseStatus;
// public class CustomWebSocketHandler extends TextWebSocketHandler {

//     @Override
//     public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//         System.out.println("Connection established with session: " + session.getId());
//         // Add any logic for a new connection here
//     }

//     @Override
//     public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//         System.out.println("Connection closed with session: " + session.getId());
//         // Add any cleanup logic for a disconnected connection here
//     }
// }
