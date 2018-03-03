# Created by L2Emu Team
import sys
from com.l2jmobius.gameserver.instancemanager           import GrandBossManager
from com.l2jmobius.gameserver.model.quest        import State
from com.l2jmobius.gameserver.model.quest        import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "8003_MeetBaium"

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st : st = self.newQuestState(player)

   baiumStatus = GrandBossManager.getInstance().getBossStatus(29020)
   if baiumStatus != 2 and st.getQuestItemsCount(4295) :
     st.exitQuest(1)
     return "31862.htm"
   else :
     npc.showChatWindow(player)
     st.exitQuest(1)
     return
   return

QUEST = Quest(-1,qn,"custom")

QUEST.addStartNpc(31862)

QUEST.addFirstTalkId(31862)

QUEST.addTalkId(31862)