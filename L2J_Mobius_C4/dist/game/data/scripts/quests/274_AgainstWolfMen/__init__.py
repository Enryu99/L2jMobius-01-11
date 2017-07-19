# Made by Mr. - Version 0.3 by DrLecter
import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

MARAKU_WEREWOLF_HEAD = 1477
NECKLACE_OF_VALOR = 1507
NECKLACE_OF_COURAGE = 1506
ADENA = 57
MARAKU_WOLFMEN_TOTEM = 1501

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [MARAKU_WEREWOLF_HEAD]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7569-03.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
   if st.getInt("cond")==0 :
     if st.getPlayer().getRace().ordinal() == 3 :
       if st.getPlayer().getLevel() > 8 :
         if st.getQuestItemsCount(NECKLACE_OF_VALOR) or st.getQuestItemsCount(NECKLACE_OF_COURAGE) :
           htmltext = "7569-02.htm"
         else :
           htmltext = "7569-07.htm"
           st.exitQuest(1)
       else :
         htmltext = "7569-01.htm"
         st.exitQuest(1)
     else :
       htmltext = "7569-00.htm"
       st.exitQuest(1)
   else :
     if st.getQuestItemsCount(MARAKU_WEREWOLF_HEAD) < 40 :
       htmltext = "7569-04.htm"
     else :
       if st.getQuestItemsCount(MARAKU_WOLFMEN_TOTEM) :
         htmltext = "7569-06.htm"
       else :
         htmltext = "7569-05.htm"
       st.exitQuest(1)
       st.playSound("ItemSound.quest_finish")
       st.giveItems(ADENA,3500)
       st.takeItems(MARAKU_WEREWOLF_HEAD,-1)
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("274_AgainstWolfMen")
   if st :
     if st.getState() != STARTED : return
     count=st.getQuestItemsCount(MARAKU_WEREWOLF_HEAD)
     if count < 40 :
       if count < 39 :
         st.playSound("ItemSound.quest_itemget")
       else:
         st.playSound("ItemSound.quest_middle")
         st.set("cond","2")
       st.giveItems(MARAKU_WEREWOLF_HEAD,1)
       if st.getRandom(100) <= 5 :
         st.giveItems(MARAKU_WOLFMEN_TOTEM,1)
   return

QUEST       = Quest(274,"274_AgainstWolfMen","Against Wolf Men")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7569)

QUEST.addTalkId(7569)

QUEST.addKillId(363)
QUEST.addKillId(364)