package edu.brown.cs.GROUP.chat;

import edu.brown.cs.GROUP.crosswordswithFriends.GUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * This class handles the chatroom for two players.
 *
 */
@WebSocket
public class TwoChatWebSocketHandler {

  /** This is a hashmap that maps the user id to the room id. */


  private static Map<Session, Integer> userRoom =
      new HashMap<Session, Integer>();

  /** This connects the user id to the chatroom.
   *
   * @param user the user id
   * @throws Exception in case it breaks */

  @OnWebSocketConnect
  public void onConnect(Session user) throws Exception {

    int nextRoomNumber = GUI.TWOPLAYERID.get();
    // roomUsers maps room#s to list of sessions
    List<Session> usersInRoom = new ArrayList<Session>();
    if (Chat.getRoomUsers().get(nextRoomNumber) != null) {
      usersInRoom = Chat.getRoomUsers().get(nextRoomNumber);
      GUI.TWOPLAYERID.getAndIncrement();
    }

    usersInRoom.add(user);
    Chat.getRoomUsers().put(nextRoomNumber, usersInRoom);

    String username = "down" + nextRoomNumber;
    // userUsernameMap maps sessions to usernames
    if (Chat.getUserUsernameMap().containsValue(username)) {
      username = "across" + nextRoomNumber;
    }
    Chat.getUserUsernameMap().put(user, username);

    // userRoom maps sessions to room#s
    userRoom.put(user, nextRoomNumber);

    Chat.broadcastStart("Server", (username + " joined the chat"),
        nextRoomNumber);
  }

  /** This method returns the room number given the uer id.
   *
   * @param user the user id
   * @return the room number */

  public static Integer getRoomNumber(Session user) {
    return userRoom.get(user);
  }

  /**
   * If one of the users close the window or leave the game it tells the other
   * player to continue as one-player game.
   * @param user the user
   * @param statusCode the status code
   * @param reason what happened
   */

  @OnWebSocketClose
  public void onClose(Session user, int statusCode, String reason) {
    // if the only person in the chat room, set roomUsers(roomNumber) to null
    String username = Chat.getUserUsernameMap().get(user);
    Chat.getUserUsernameMap().remove(user);

    Integer roomNumber = userRoom.get(user);
    userRoom.remove(user);

    List<Session> usersInChat = Chat.getRoomUsers().get(roomNumber);
    if (usersInChat.size() == 1) {
      Chat.getRoomUsers().remove(roomNumber);
      GUI.removeCrossword(roomNumber);
    } else {
      usersInChat.remove(user);
    }
    Chat.broadcastMessage("Server", (username + " left the chat"), roomNumber);
  }

  /** This function determines what message to send to the chatroom depending on
   * whether the user requested data, a letter, or an anagram by clicking on the
   * buttons.
   *
   * @param user the user id
   * @param message the message */
  @OnWebSocketMessage
  public void onMessage(Session user, String message) {
    if (message.startsWith("DATA")) {
      Chat.broadcastCorrect(message, userRoom.get(user));
    } else if (message.startsWith("**ALL**")) {
      Chat.broadcastAll(user, userRoom.get(user));
    } else if (message.startsWith("LETTER")) {
      Chat.broadcastLetter(message, userRoom.get(user));
    } else if (message.startsWith("ANAGRAM")) {
      Chat.broadcastAnagram(message, userRoom.get(user));
    } else if (message.startsWith("**CONVERT**")) {
      Chat.broadcastConvert(user, userRoom.get(user));
    } else if (message.startsWith("**END**")) {
      Chat.broadcastEnd(message, user, userRoom.get(user));
    } else {
      Chat.broadcastMessage(Chat.getUserUsernameMap().get(user), message,
          userRoom.get(user));
    }
  }

}
