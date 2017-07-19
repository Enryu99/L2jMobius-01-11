# Made by Mr. - Version 0.3 by DrLecter
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

BONE_FRAGMENT = 809
ADENA = 57

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [BONE_FRAGMENT]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7359-04.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.setState(STARTING)
     st.set("cond","0")
   if st.getInt("cond")==0 :
     if st.getPlayer().getRace().ordinal() != 2 :
       htmltext = "7359-00.htm"
       st.exitQuest(1)
     elif st.getPlayer().getLevel() >= 10 :
       htmltext = "7359-03.htm"
     else:
       htmltext = "7359-02.htm"
       st.exitQuest(1)
   else :
     if st.getQuestItemsCount(BONE_FRAGMENT)<10 :
       htmltext = "7359-05.htm"
     else :
       htmltext = "7359-06.htm"
       st.giveItems(ADENA,8470)
       st.takeItems(BONE_FRAGMENT,-1)
       st.exitQuest(1)
       st.playSound("ItemSound.quest_finish")
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("320_BonesTellFuture")
   if st :
     if st.getState() != STARTED : return
     count=st.getQuestItemsCount(BONE_FRAGMENT)
     if count<10 and st.getRandom(10)>7 :
        st.giveItems(BONE_FRAGMENT,1)
        if count == 9 :
          st.playSound("ItemSound.quest_middle")
          st.set("cond","2")
        else :
          st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(320,"320_BonesTellFuture","Bones Tell Future")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7359)

QUEST.addTalkId(7359)

QUEST.addKillId(517)
QUEST.addKillId(518)