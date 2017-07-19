# Made by disKret
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "241_PossessorOfAPreciousSoul_1"

#NPC
STEDMIEL = 7692
GABRIELLE = 7753
GILMORE = 7754
KANTABILON = 8042
NOEL = 8272
RAHORAKTI = 8336
TALIEN = 8739
CARADINE = 8740
VIRGIL = 8742
KASSANDRA = 8743
OGMAR = 8744

#QUEST ITEM
LEGEND_OF_SEVENTEEN = 7587
MALRUK_SUCCUBUS_CLAW = 7597
ECHO_CRYSTAL = 7589
POETRY_BOOK = 7588
CRIMSON_MOSS = 7598
RAHORAKTIS_MEDICINE = 7599
LUNARGENT = 6029
HELLFIRE_OIL = 6033
VIRGILS_LETTER = 7677

#CHANCE
CRIMSON_MOSS_CHANCE = 5
MALRUK_SUCCUBUS_CLAW_CHANCE = 10

#MOB
BARAHAM = 5113

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [LEGEND_OF_SEVENTEEN, MALRUK_SUCCUBUS_CLAW, ECHO_CRYSTAL, POETRY_BOOK, CRIMSON_MOSS, RAHORAKTIS_MEDICINE]

 def onEvent (self,event,st) :
   htmltext = event
   cond = st.getInt("cond")
   if not st.getPlayer().isSubClassActive() : return
   if event == "8739-4.htm" :
     if cond == 0 :
       st.setState(STARTED)
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
   elif event == "7753-2.htm" :
     if cond == 1 :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
   elif event == "7754-2.htm" :
     if cond == 2 :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
   elif event == "8739-8.htm" :
     if cond == 4 and st.getQuestItemsCount(LEGEND_OF_SEVENTEEN) :
       st.set("cond","5")
       st.takeItems(LEGEND_OF_SEVENTEEN,1)
       st.playSound("ItemSound.quest_middle")
   elif event == "8042-2.htm" :
     if cond == 5 :
       st.set("cond","6")
       st.playSound("ItemSound.quest_middle")
   elif event == "8042-5.htm" :
     if cond == 7 and st.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) >= 10 :
       st.set("cond","8")
       st.takeItems(MALRUK_SUCCUBUS_CLAW,10)
       st.giveItems(ECHO_CRYSTAL,1)
       st.playSound("ItemSound.quest_middle")
   elif event == "8739-12.htm" :
     if cond == 8 and st.getQuestItemsCount(ECHO_CRYSTAL) :
       st.set("cond","9")
       st.takeItems(ECHO_CRYSTAL,1)
       st.playSound("ItemSound.quest_accept")
   elif event == "7692-2.htm" :
     if cond == 9 :
       st.set("cond","10")
       st.giveItems(POETRY_BOOK,1)
       st.playSound("ItemSound.quest_accept")
   elif event == "8739-15.htm" :
     if cond == 10 and st.getQuestItemsCount(POETRY_BOOK) :
       st.set("cond","11")
       st.takeItems(POETRY_BOOK,1)
       st.playSound("ItemSound.quest_accept")
   elif event == "8742-2.htm" :
     if cond == 11 :
       st.set("cond","12")
       st.playSound("ItemSound.quest_accept")
   elif event == "8744-2.htm" :
     if cond == 12 :
       st.set("cond","13")
       st.playSound("ItemSound.quest_accept")
   elif event == "8336-2.htm" :
     if cond == 13 :
       st.set("cond","14")
       st.playSound("ItemSound.quest_accept")
   elif event == "8336-5.htm" :
     if cond == 15 and st.getQuestItemsCount(CRIMSON_MOSS) :
       st.set("cond","16")
       st.takeItems(CRIMSON_MOSS,5)
       st.giveItems(RAHORAKTIS_MEDICINE,1)
       st.playSound("ItemSound.quest_accept")
   elif event == "8743-2.htm" :
     if cond == 16 and st.getQuestItemsCount(RAHORAKTIS_MEDICINE) :
       st.set("cond","17")
       st.takeItems(RAHORAKTIS_MEDICINE,1)
       st.playSound("ItemSound.quest_accept")
   elif event == "8742-5.htm" :
     if cond == 17 :
       st.set("cond","18")
       st.playSound("ItemSound.quest_accept")
   elif event == "8740-2.htm" :
     if cond == 18 :
       st.set("cond","19")
       st.playSound("ItemSound.quest_accept")
   elif event == "8272-2.htm" :
     if cond == 19 :
       st.set("cond","20")
       st.playSound("ItemSound.quest_accept")
   elif event == "8272-5.htm" :
     if cond == 20 and st.getQuestItemsCount(LUNARGENT) >= 5 and st.getQuestItemsCount(HELLFIRE_OIL) :
       st.takeItems(LUNARGENT,5)
       st.takeItems(HELLFIRE_OIL,1)
       st.set("cond","21")
       st.playSound("ItemSound.quest_accept")
     else :
       htmltext = "8272-4.htm"
   elif event == "8740-5.htm" :
     if cond == 21 :
       st.giveItems(VIRGILS_LETTER,1)
       st.addExpAndSp(263043,0)
       st.set("cond","0")
       st.playSound("ItemSound.quest_finish")
       st.setState(COMPLETED)
   return htmltext

 def onTalk (Self,npc,st) :
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   npcId = npc.getNpcId()
   id = st.getState()
   if npcId != TALIEN and id != STARTED :
       return htmltext
   cond = st.getInt("cond")
   if npcId == TALIEN :
       if cond == 0 :
         if id == COMPLETED :
           htmltext = "<html><body>This quest has already been completed.</body></html>"
         elif st.getPlayer().getLevel() >= 50 and st.getPlayer().isSubClassActive(): 
           htmltext = "8739-1.htm"
         else :
           htmltext = "8739-2.htm"
           st.exitQuest(1)
         if not st.getPlayer().isSubClassActive() :
           htmltext = "<html><body>This quest may only be undertaken by sub-class characters of level 50 or above.</body></html>"
       elif cond == 1 :
         htmltext = "8739-5.htm"
       elif cond == 4 and st.getQuestItemsCount(LEGEND_OF_SEVENTEEN) == 1 :
         htmltext = "8739-6.htm"
       elif cond == 5 :
         htmltext = "8739-9.htm"
       elif cond == 8 and st.getQuestItemsCount(ECHO_CRYSTAL) == 1 :
         htmltext = "8739-11.htm"
       elif cond == 9 :
         htmltext = "8739-13.htm"
       elif cond == 10 and st.getQuestItemsCount(POETRY_BOOK) == 1 :
         htmltext = "8739-14.htm"
       elif cond == 11 :
         htmltext = "8739-16.htm"
   elif st.getPlayer().isSubClassActive() :
     if npcId == GABRIELLE :
       if cond == 1 :
         htmltext = "7753-1.htm"
       elif cond == 2 :
         htmltext = "7753-3.htm"
     elif npcId == GILMORE :
       if cond == 2 :
         htmltext = "7754-1.htm"
       elif cond == 3 :
         htmltext = "7754-3.htm"
     elif npcId == KANTABILON :
       if cond == 5 :
         htmltext = "8042-1.htm"
       elif cond == 6 :
         htmltext = "8042-4.htm"
       elif cond == 7 and st.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) == 10 :
         htmltext = "8042-3.htm"
       elif cond == 8 :
         htmltext = "8042-6.htm"
     elif npcId == STEDMIEL :
       if cond == 9 :
         htmltext = "7692-1.htm"
       elif cond == 10 :
         htmltext = "7692-3.htm"
     elif npcId == VIRGIL :
       if cond == 11 :
         htmltext = "8742-1.htm"
       elif cond == 12 :
         htmltext = "8742-3.htm"
       elif cond == 17 :
         htmltext = "8742-4.htm"
       elif cond == 18 :
         htmltext = "8742-6.htm"
     elif npcId == OGMAR :
       if cond == 12 :
         htmltext = "8744-1.htm"
       elif cond == 13 :
         htmltext = "8744-3.htm"
     elif npcId == RAHORAKTI :
       if cond == 13 :
         htmltext = "8336-1.htm"
       elif cond == 14 :
         htmltext = "8336-4.htm"
       elif cond == 15 and st.getQuestItemsCount(CRIMSON_MOSS) == 5 :
         htmltext = "8336-3.htm"
       elif cond == 16 :
         htmltext = "8336-6.htm"
     elif npcId == KASSANDRA :
       if cond == 16 and st.getQuestItemsCount(RAHORAKTIS_MEDICINE) == 1 :
         htmltext = "8743-1.htm"
       elif cond == 17 :
         htmltext = "8743-3.htm"
     elif npcId == CARADINE :
       if cond == 18 :
         htmltext = "8740-1.htm"
       elif cond == 19 :
         htmltext = "8740-3.htm"
       elif cond == 21 :
         htmltext = "8740-4.htm"
     elif npcId == NOEL :
       if cond == 19 :
         htmltext = "8272-1.htm"
       elif cond == 20 and st.getQuestItemsCount(LUNARGENT) < 5 and not st.getQuestItemsCount(HELLFIRE_OIL) :
         htmltext = "8272-4.htm"
       elif cond == 20 and st.getQuestItemsCount(LUNARGENT) >= 5 and st.getQuestItemsCount(HELLFIRE_OIL) :
         htmltext = "8272-3.htm"
       elif cond == 21 :
         htmltext = "8272-7.htm"
   else :
     htmltext = "<html><body>This quest may only be undertaken by sub-class characters of level 50 or above.</body></html>"
   return htmltext

 def onKill (self,npc,player,isPet) :
   npcId = npc.getNpcId()
   if npcId == BARAHAM :
     # get a random party member who is doing this quest and is at cond == 3
     partyMember = self.getRandomPartyMember(player, "3")
     if partyMember :
         st = partyMember.getQuestState(qn)
         st.set("cond","4")
         st.giveItems(LEGEND_OF_SEVENTEEN,1)
         st.playSound("ItemSound.quest_itemget")
   elif npcId in [244,245,283,284] :
     # get a random party member who is doing this quest and is at cond == 6 
     partyMember = self.getRandomPartyMember(player, "6")
     if partyMember :
         st = partyMember.getQuestState(qn)
         chance = st.getRandom(100)
         if MALRUK_SUCCUBUS_CLAW_CHANCE >= chance and st.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) < 10 :
           st.giveItems(MALRUK_SUCCUBUS_CLAW,1)
           st.playSound("ItemSound.quest_itemget")
           if st.getQuestItemsCount(MALRUK_SUCCUBUS_CLAW) == 10 :
             st.set("cond","7")
             st.playSound("ItemSound.quest_middle")
   elif npcId in [1511,1512] :
     # get a random party member who is doing this quest and is at cond == 14
     partyMember = self.getRandomPartyMember(player, "14")
     if partyMember :
         st = partyMember.getQuestState(qn)
         chance = st.getRandom(100)
         if CRIMSON_MOSS_CHANCE >= chance and st.getQuestItemsCount(CRIMSON_MOSS) < 5 :
           st.giveItems(CRIMSON_MOSS,1)
           st.playSound("ItemSound.quest_itemget")
           if st.getQuestItemsCount(CRIMSON_MOSS) == 5 :
             st.set("cond","15")
             st.playSound("ItemSound.quest_middle")
   return

QUEST       = Quest(241,qn,"Possessor Of A Precious Soul - 1")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(TALIEN)
QUEST.addTalkId(TALIEN)
QUEST.addTalkId(STEDMIEL)
QUEST.addTalkId(GABRIELLE)
QUEST.addTalkId(GILMORE)
QUEST.addTalkId(KANTABILON)
QUEST.addTalkId(NOEL)
QUEST.addTalkId(RAHORAKTI)
QUEST.addTalkId(CARADINE)
QUEST.addTalkId(VIRGIL)
QUEST.addTalkId(KASSANDRA)
QUEST.addTalkId(OGMAR)

QUEST.addKillId(BARAHAM)
QUEST.addKillId(244)
QUEST.addKillId(245)
QUEST.addKillId(283)
QUEST.addKillId(284)

QUEST.addKillId(1511)
QUEST.addKillId(1512)