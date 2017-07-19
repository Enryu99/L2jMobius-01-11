# Made by Mr. - Version 0.3 by DrLecter
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

CHRYSOLITE_ORE = 1488
TORN_MAP_FRAGMENT = 1489
HIDDEN_VEIN_MAP = 1490
ADENA = 57
SOULSHOT_FOR_BEGINNERS = 5789

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [HIDDEN_VEIN_MAP, CHRYSOLITE_ORE, TORN_MAP_FRAGMENT]

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7535-03.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    elif event == "7535-06.htm" :
      st.takeItems(TORN_MAP_FRAGMENT,-1)
      st.exitQuest(1)
      st.playSound("ItemSound.quest_finish")
    elif event == "7539-02.htm" :
      if st.getQuestItemsCount(TORN_MAP_FRAGMENT) >=4 :
        htmltext = "7539-03.htm"
        st.giveItems(HIDDEN_VEIN_MAP,1)
        st.takeItems(TORN_MAP_FRAGMENT,4)
    return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
   if npcId == 7535 :
     if st.getInt("cond")==0 :
       if st.getPlayer().getRace().ordinal() != 4 :
         htmltext = "7535-00.htm"
         st.exitQuest(1)
       elif st.getPlayer().getLevel() >= 6 :
         htmltext = "7535-02.htm"
         return htmltext
       else:
         htmltext = "7535-01.htm"
         st.exitQuest(1)
     else :
       if st.getQuestItemsCount(CHRYSOLITE_ORE)==0 :
         if st.getQuestItemsCount(HIDDEN_VEIN_MAP)==0 :
           htmltext = "7535-04.htm"
         else :
           htmltext = "7535-08.htm"
           qs = st.getPlayer().getQuestState("255_Tutorial")
           if qs :
              newbiegift=qs.getInt("newbiegift")
              if newbiegift != 3 and st.getPlayer().getNewbieState() == 1 :
                 st.showQuestionMark(26)
                 st.playTutorialVoice("tutorial_voice_026")
                 st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
                 qs.set("newbiegift","3")
           st.giveItems(ADENA,st.getQuestItemsCount(HIDDEN_VEIN_MAP)*1000)
           st.takeItems(HIDDEN_VEIN_MAP,-1)
       else :
         if st.getQuestItemsCount(HIDDEN_VEIN_MAP)==0 :
           htmltext = "7535-05.htm"
           qs = st.getPlayer().getQuestState("255_Tutorial")
           if qs :
              newbiegift=qs.getInt("newbiegift")
              if newbiegift != 3 and st.getPlayer().getNewbieState() == 1 :
                 st.showQuestionMark(26)
                 st.playTutorialVoice("tutorial_voice_026")
                 st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
                 qs.set("newbiegift","3")
           st.giveItems(ADENA,st.getQuestItemsCount(CHRYSOLITE_ORE)*10)
           st.takeItems(CHRYSOLITE_ORE,-1)
         else :
           htmltext = "7535-09.htm"
           qs = st.getPlayer().getQuestState("255_Tutorial")
           if qs :
              newbiegift=qs.getInt("newbiegift")
              if newbiegift != 3 and st.getPlayer().getNewbieState() == 1 :
                 st.showQuestionMark(26)
                 st.playTutorialVoice("tutorial_voice_026")
                 st.giveItems(SOULSHOT_FOR_BEGINNERS,6000)
                 qs.set("newbiegift","3")
           st.giveItems(ADENA,st.getQuestItemsCount(CHRYSOLITE_ORE)*10+st.getQuestItemsCount(HIDDEN_VEIN_MAP)*1000)
           st.takeItems(HIDDEN_VEIN_MAP,-1)
           st.takeItems(CHRYSOLITE_ORE,-1)
   elif npcId == 7539 :
      htmltext = "7539-01.htm"
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("293_HiddenVein")
   if st :
     if st.getState() != STARTED : return
     n = st.getRandom(100)
     if n > 50 :
       st.giveItems(CHRYSOLITE_ORE,1)
       st.playSound("ItemSound.quest_itemget")
     elif n < 5 :
       st.giveItems(TORN_MAP_FRAGMENT,1)
       st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(293,"293_HiddenVein","Hidden Vein")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7535)

QUEST.addTalkId(7535)

QUEST.addTalkId(7539)

QUEST.addKillId(446)
QUEST.addKillId(447)
QUEST.addKillId(448)