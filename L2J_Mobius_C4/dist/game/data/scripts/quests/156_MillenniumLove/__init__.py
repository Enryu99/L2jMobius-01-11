# Made by Mr. Have fun! Version 0.2
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

RYLITHS_LETTER_ID = 1022
THEONS_DIARY_ID = 1023
ADENA_ID = 57
SWIFT_ATTACK_POTION_ID = 735

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [RYLITHS_LETTER_ID, THEONS_DIARY_ID]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1" :
        st.set("id","0")
        if st.getPlayer().getLevel() >= 15 :
          htmltext = "7368-06.htm"
          st.giveItems(RYLITHS_LETTER_ID,1)
          st.set("cond","1")
          st.setState(STARTED)
          st.playSound("ItemSound.quest_accept")
        else:
          htmltext = "7368-05.htm"
    elif event == "156_1" :
            st.takeItems(RYLITHS_LETTER_ID,st.getQuestItemsCount(RYLITHS_LETTER_ID))
            if st.getQuestItemsCount(THEONS_DIARY_ID) == 0 :
              st.giveItems(THEONS_DIARY_ID,1)
            htmltext = "7369-03.htm"
    elif event == "156_2" and st.getInt("onlyone") == 0 :
            if st.getInt("id") != 156 :
              st.set("id","156")
              st.takeItems(RYLITHS_LETTER_ID,st.getQuestItemsCount(RYLITHS_LETTER_ID))
              st.set("cond","0")
              st.setState(COMPLETED)
              st.playSound("ItemSound.quest_finish")
              st.set("onlyone","1")
              st.giveItems(ADENA_ID,3000)
              htmltext = "7369-04.htm"
    return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.setState(STARTING)
     st.set("cond","0")
     st.set("onlyone","0")
     st.set("id","0")
   if npcId == 7368 and st.getInt("cond")==0 and st.getInt("onlyone")==0 :
      htmltext = "7368-04.htm"
   elif npcId == 7368 and st.getInt("cond")==0 and st.getInt("onlyone")==1 :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 7368 and st.getInt("cond")!=0 and st.getQuestItemsCount(RYLITHS_LETTER_ID)!=0 :
        htmltext = "7368-07.htm"
   elif npcId == 7369 and st.getInt("cond")!=0 and st.getQuestItemsCount(RYLITHS_LETTER_ID)!=0 :
        htmltext = "7369-02.htm"
   elif npcId == 7369 and st.getInt("cond")!=0 and st.getQuestItemsCount(THEONS_DIARY_ID)!=0 :
        htmltext = "7369-05.htm"
   elif npcId == 7368 and st.getInt("cond")!=0 and st.getQuestItemsCount(THEONS_DIARY_ID)!=0 and st.getInt("onlyone")==0 :
        if st.getInt("id") != 156 :
          st.set("id","156")
          st.takeItems(THEONS_DIARY_ID,st.getQuestItemsCount(THEONS_DIARY_ID))
          st.set("cond","0")
          st.setState(COMPLETED)
          st.playSound("ItemSound.quest_finish")
          st.set("onlyone","1")
          st.addExpAndSp(3600,0)
          st.giveItems(SWIFT_ATTACK_POTION_ID,2)
          htmltext = "7368-08.htm"
   return htmltext

QUEST       = Quest(156,"156_MillenniumLove","Millennium Love")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7368)

QUEST.addTalkId(7368)
QUEST.addTalkId(7369)