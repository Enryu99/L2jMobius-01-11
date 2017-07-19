# Fix by Cromir for Kilah
# cleanup by DrLecter for the Official L2J Datapack Project (adapted for L2JLisvus by roko91).
# Visit http://forum.l2jdp.com for more details.

import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "38_DragonFangs"

#Quest items
FEATHER_ORNAMENT,TOOTH_OF_TOTEM,TOOTH_OF_DRAGON,LETTER_OF_IRIS,LETTER_OF_ROHMER = range(7173,7178)

#Drop info:
DROPLIST={1100:[1,FEATHER_ORNAMENT,100,100],357:[1,FEATHER_ORNAMENT,100,100],1101:[6,TOOTH_OF_DRAGON,50,50],356:[6,TOOTH_OF_DRAGON,50,50]}
#Rewards: item,adena
REWARDS=[[45,5200],[627,1500],[1123,3200],[605,3200]]
#NPC
LUIS=7386
IRIS=7034
ROHMER=7344

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = range(7173,7178)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "7386-02.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif event == "7386-04.htm" :
       st.set("cond","3")
       st.takeItems(FEATHER_ORNAMENT,100)
       st.giveItems(TOOTH_OF_TOTEM,1)
       st.playSound("ItemSound.quest_middle")
    elif event == "7034-02a.htm" and st.getQuestItemsCount(TOOTH_OF_TOTEM) :
       htmltext = "7034-02.htm"
       st.set("cond","4")
       st.takeItems(TOOTH_OF_TOTEM,1)
       st.giveItems(LETTER_OF_IRIS,1)
       st.playSound("ItemSound.quest_middle")
    elif event == "7344-02a.htm" and st.getQuestItemsCount(LETTER_OF_IRIS) :
       htmltext = "7344-02.htm"
       st.set("cond","5")
       st.takeItems(LETTER_OF_IRIS,1)
       st.giveItems(LETTER_OF_ROHMER,1)
       st.playSound("ItemSound.quest_middle")
    elif event == "7034-04a.htm"and st.getQuestItemsCount(LETTER_OF_ROHMER) :
       st.takeItems(LETTER_OF_ROHMER,1)
       htmltext = "7034-04.htm"
       st.set("cond","6")
       st.playSound("ItemSound.quest_middle")
    elif event == "7034-06a.htm" and st.getQuestItemsCount(TOOTH_OF_DRAGON) == 50 :
       htmltext = "7034-06.htm"
       st.unset("cond")
       st.takeItems(TOOTH_OF_DRAGON,50)
       st.playSound("ItemSound.quest_finish")
       item,adena=REWARDS[st.getRandom(len(REWARDS))]
       st.giveItems(item,1)
       st.giveItems(57,adena)
       st.setState(COMPLETED)
    return htmltext

 def onTalk (self,npc,st):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   npcId = npc.getNpcId()
   id = st.getState()
   cond=st.getInt("cond")
   if id == COMPLETED :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == LUIS :
      if cond == 0 :
        if st.getPlayer().getLevel() >= 19 :
           htmltext = "7386-01.htm"
        else :
           st.exitQuest(1)
           htmltext = "7386-01a.htm"
      elif cond == 1 :
        htmltext = "7386-02a.htm"
      elif cond == 2 :
        htmltext = "7386-03.htm"
      elif cond == 3 :
        htmltext = "7386-03a.htm"
   elif npcId == IRIS :
      if cond == 3 :
        htmltext = "7034-01.htm"
      elif cond == 4 :
        htmltext = "7034-02b.htm"
      elif cond == 5 :
        htmltext = "7034-03.htm"
      elif cond == 6 :
        htmltext = "7034-05a.htm"
      elif cond == 7 :
        htmltext = "7034-05.htm"
   elif npcId == ROHMER :
      if cond == 4 :
         htmltext = "7344-01.htm"
      elif cond == 5 :
         htmltext = "7344-03.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return
   if st.getState() != STARTED : return
   cond = st.getInt("cond")
   cond,item,max,chance=DROPLIST[npc.getNpcId()]
   count=st.getQuestItemsCount(item)
   if st.getInt("cond") == cond and count < max and st.getRandom(100) < chance :
      st.giveItems(item,1)
      if count == max-1 :
         st.playSound("ItemSound.quest_middle")
         st.set("cond",str(cond+1))
      else :
         st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(38,qn,"Dragon Fangs")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(LUIS)

QUEST.addTalkId(LUIS)
QUEST.addTalkId(IRIS)
QUEST.addTalkId(ROHMER)

for mob in DROPLIST.keys():
    QUEST.addKillId(mob)