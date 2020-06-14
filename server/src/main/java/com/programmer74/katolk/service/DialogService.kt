package com.programmer74.katolk.service

import com.programmer74.katolk.dao.DialogueEntity
import com.programmer74.katolk.dao.DialogueParticipantEntity
import com.programmer74.katolk.dao.MessageEntity
import com.programmer74.katolk.dao.User
import com.programmer74.katolk.dto.DialogueDto
import com.programmer74.katolk.dto.MessageDto
import com.programmer74.katolk.dto.UserDto
import com.programmer74.katolk.repository.DialogueParticipantRepository
import com.programmer74.katolk.repository.DialogueRepository
import javassist.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class DialogService(
  val dialogueRepository: DialogueRepository,
  val dialogueParticipantRepository: DialogueParticipantRepository,
  val userService: UserService,
  val messagesService: MessagesService
) {

  fun createDialogue(creator: User, name: String): DialogueEntity {
    var dialogue = DialogueEntity(0, creator.safeId(), name)
    dialogue = dialogueRepository.save(dialogue)
    val participant = DialogueParticipantEntity(0, dialogue.safeId(), creator.safeId())
    dialogueParticipantRepository.save(participant)
    return dialogue
  }

  fun createDialogue(creator: User, second: User): DialogueEntity {
    val dialogue = createDialogue(creator, "PM with ${second.username}")
    addParticipant(dialogue, second)
    return dialogue
  }

  fun addParticipant(dialogue: DialogueEntity, user: User): DialogueParticipantEntity {
    val participant = DialogueParticipantEntity(0, dialogue.safeId(), user.safeId())
    return dialogueParticipantRepository.save(participant)
  }

  fun getDialogs(user: User): List<DialogueEntity> {
    val participatesIn =
        dialogueParticipantRepository.findAllByUserID(user.safeId())!!.map { it.dialogueID }
    return dialogueRepository.findByIdIn(participatesIn)!!
  }

  fun getParticipants(dialogue: DialogueEntity): List<User> {
    val participants =
        dialogueParticipantRepository.findAllByDialogueID(dialogue.safeId()) ?: return emptyList()
    return userService.findUsersByIds(participants.map { it.userID }.toList()).toList()
  }

  fun getDialogRepresentations(user: User): List<DialogueDto> {
    val belongsToDialogsIDs =
        dialogueParticipantRepository.findAllByUserID(user.safeId())?.map { it.dialogueID }
          ?: return emptyList()
    val belongsToDialogs = dialogueRepository.findByIdIn(belongsToDialogsIDs)

    return belongsToDialogs!!.map {
      DialogueDto(
          it.safeId(),
          it.creatorId,
          it.name,
          dialogueParticipantRepository.findAllByDialogueID(it.safeId())!!
              .map { participant -> participant.userID }
              .map { userId -> UserDto.from(userService.findUserById(userId)) }
              .toList(),
          messagesService.latestMessageInDialogueAsJson(it),
          messagesService.messageRepository.findByDialogueIDAndWasRead(it.safeId(), false).size)
    }.sortedByDescending {
      if (it.latestMessage != null) it.latestMessage.date else 0
    }
  }

  private fun getDialogueOrNull(userId: Long, dialogueId: Long): DialogueEntity? {
    val participants = dialogueParticipantRepository.findAllByDialogueID(dialogueId)!!
    if (participants.none { it.userID == userId }) {
      return null
    }
    return dialogueRepository.findByIdOrNull(dialogueId)
  }

  fun getMessagesInDialogueAsDto(userId: Long, dialogueId: Long): List<MessageDto> {
    val dialogue = getDialogueOrNull(userId, dialogueId) ?: return emptyList()
    return messagesService.messagesInDialogueAsJson(dialogue)
  }

  fun markMessagesInDialogueAsRead(userId: Long, dialogueId: Long) {
    val dialogue = getDialogueOrNull(userId, dialogueId) ?: return
    val unreadMessages = messagesService.unreadMessagesInDialogue(dialogue)
    if (unreadMessages.isNotEmpty()) {
      messagesService.markMessagesInDialogueAsRead(unreadMessages)
      //TODO: fix other user notifications
      //
      //      val userIds = participants.map { it.userID }
      //      val users = users.repository.findAllById(userIds)
      //          .toList()
      //
      //      users.forEach { websocketHandler.notifyUserAboutNewMessage(it) }
    }
  }

  fun sendMessage(user: User, message: MessageEntity): MessageEntity {
    val dialogueId = message.dialogueID
    val dialogue = getDialogueOrNull(user.safeId(), dialogueId)
      ?: throw NotFoundException("no dialogue or no access to $dialogueId")

    val sentMessage = messagesService.sendMessage(user, dialogue, message.body)

    //TODO: fix other user notifications
    //
    //      val userIds = participants.map { it.userID }
    //      val users = users.repository.findAllById(userIds)
    //          .toList()
    //
    //      users.forEach { websocketHandler.notifyUserAboutNewMessage(it) }
    return sentMessage
  }

  @PostConstruct
  fun initUsers() {
    //TODO: do this by migration
    //    val admin = users.repository.findByUsername("admin")!!
    //    val user1 = users.repository.findByUsername("user1")!!
    //    val user2 = users.repository.findByUsername("user2")!!
    //
    //    val dialogue = createDialogue(user1, user2)
    //
    //    val conference = createDialogue(admin, "conference")
    //    addParticipant(conference, user1)
    //    addParticipant(conference, user2)
    //
    //    val talkToUrself = createDialogue(user1, "Private chat")
    //
    //    messages.sendMessage(admin, conference, "Broadcast to users1&2")
    //    messages.sendMessage(user1, conference, "Reply from user1")
    //    val msg =
    //        messages.sendMessage(user1, talkToUrself, "This is your private dialogue with yourself")
    //    msg.wasRead = true
    //    messages.messageRepository.save(msg)
  }
}