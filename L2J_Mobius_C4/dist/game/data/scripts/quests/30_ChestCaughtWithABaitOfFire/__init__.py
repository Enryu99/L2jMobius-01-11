# Made by Ethernaly ethernaly@email.it (adapted for L2JLisvus by roko91)
# cleanup by DrLecter for the Official L2J Datapack Project.
# Visit http://forum.l2jdp.com for more details.

import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "30_ChestCaughtWithABaitOfFire"

#NPC
LINNAEUS = 8577
RUKAL    = 7629

#QUEST ITEM and REWARD
RED_TREASURE_BOX    = 6511
RUKAL_MUSICAL       = 7628
PROTECTION_NECKLACE = 916

class Quest (JQuest) :

  def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)
  
  def onEvent(self, event, st):
    htmltext = event
    if event== "8577-02.htm" :
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
    elif event == "8577-04a.htm" and st.getQuestItemsCount(RED_TREASURE_BOX) :
      htmltext="8577-04.htm"
      st.playSound("ItemSound.quest_middle")
      st.giveItems(RUKAL_MUSICAL, 1)
      st.takeItems(RED_TREASURE_BOX,-1)
      st.set("cond","2")
    elif event == "7629-02.htm" and st.getQuestItemsCount(RUKAL_MUSICAL) :
      htmltext = "7629-03.htm"
      st.playSound("ItemSound.quest_finish")
      st.giveItems(PROTECTION_NECKLACE, 1)
      st.takeItems(RUKAL_MUSICAL,-1)
      st.unset("cond")
      st.setState(COMPLETED)
    return htmltext

  def onTalk(self,npc,st):
    htmltext="<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
    npcId=npc.getNpcId()
    id = st.getState()
    if id == CREATED :
      req = st.getPlayer().getQuestState("53_LinnaeusSpecialBait")
      if req : reqst = req.getState()
      if st.getPlayer().getLevel() >= 60 and req and reqst.getName() == 'Completed' :
        htmltext = "8577-01.htm"
      else :
        st.exitQuest(1)
        htmltext = "8577-00.htm"
    elif id == STARTED :
      cond = st.getInt("cond")
      if npcId == LINNAEUS :
        if cond == 1 :
          if st.getQuestItemsCount(RED_TREASURE_BOX) :
            htmltext = "8577-03.htm"
          else :
            htmltext = "8577-03a.htm"
        else :
          htmltext = "8577-05.htm"
      else :
        htmltext = "7629-01.htm"
    elif id == COMPLETED :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
    return htmltext

QUEST=Quest(30,qn,"Chest Caught With A Bait Of Fire")
CREATED=State('Start', QUEST)
STARTED=State('Started', QUEST)
COMPLETED=State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(LINNAEUS)
QUEST.addTalkId(LINNAEUS)
QUEST.addTalkId(RUKAL)