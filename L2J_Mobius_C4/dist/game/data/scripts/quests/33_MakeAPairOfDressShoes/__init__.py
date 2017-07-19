# Made by disKret
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

LEATHER = 1882
THREAD = 1868
ADENA = 57
DRESS_SHOES_BOX = 7113

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
   htmltext = event
   if event == "7838-1.htm" :
     st.set("cond","1")
     st.setState(STARTED)
     st.playSound("ItemSound.quest_accept")
   if event == "8520-1.htm" :
     st.set("cond","2")
   if event == "7838-3.htm" :
     st.set("cond","3")
   if event == "7838-5.htm" :
     if st.getQuestItemsCount(LEATHER) >= 200 and st.getQuestItemsCount(THREAD) >= 600 and st.getQuestItemsCount(ADENA) >= 200000 :
       st.takeItems(LEATHER,200)
       st.takeItems(THREAD,600)
       st.takeItems(ADENA,200000)
       st.set("cond","4")
     else :
       htmltext = "You don't have enough materials"
   if event == "7164-1.htm" :
     if st.getQuestItemsCount(ADENA) >= 300000 :
       st.takeItems(ADENA,300000)
       st.set("cond","5")
     else :
       htmltext = "You don't have enough materials."
   if event == "7838-7.htm" :
     st.giveItems(DRESS_SHOES_BOX,1)
     st.playSound("ItemSound.quest_finish")
     st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   npcId = npc.getNpcId()
   id = st.getState()
   if id == CREATED :
     st.set("cond","0")
   cond = st.getInt("cond")
   if npcId == 7838 and cond == 0 and st.getQuestItemsCount(DRESS_SHOES_BOX) == 0 :
     fwear=st.getPlayer().getQuestState("37_PleaseMakeMeFormalWear")
     if fwear :
       if fwear.get("cond") == "7" :
         htmltext = "7838-0.htm"
       else:
         st.exitQuest(1)
     else:
       st.exitQuest(1)
   elif npcId == 8520 and cond == 1 :
     htmltext = "8520-0.htm"
   elif npcId == 7838 and cond == 2 :
     htmltext = "7838-2.htm"
   elif npcId == 7838 and cond == 3 :
     htmltext = "7838-4.htm"
   elif npcId == 7164 and cond == 4 :
     htmltext = "7164-0.htm"
   elif npcId == 7838 and cond == 5 :
     htmltext = "7838-6.htm"
   return htmltext

QUEST       = Quest(33,"33_MakeAPairOfDressShoes","Make A Pair Of Dress Shoes")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7838)
QUEST.addTalkId(7838)
QUEST.addTalkId(7164)
QUEST.addTalkId(8520)