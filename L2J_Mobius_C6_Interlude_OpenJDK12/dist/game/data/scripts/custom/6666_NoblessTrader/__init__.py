import sys
from org.l2jmobius.gameserver.model.actor.instance import L2PcInstance
from org.l2jmobius.gameserver.model.actor.instance import L2NpcInstance
from java.util import Iterator
from org.l2jmobius.gameserver.model.quest import State
from org.l2jmobius.gameserver.model.quest import QuestState
from org.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

qn = "6666_NoblessTrader"

NPC=[66666]
NOBLESS_TIARA=7694
GOLD_BAR=3470
QuestId     = 6666
QuestName   = "NoblessTrade"
QuestDesc   = "custom"
InitialHtml = "31739-1.htm"

print "INFO  Nobless Trader (66666) Enabled..."

class Quest (JQuest) :

	def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

	def onEvent(self,event,st):
               htmltext = "<html><head><body>I have nothing to say you</body></html>"
               cond = st.getInt("cond")
               count=st.getQuestItemsCount(GOLD_BAR)
               if event == "31739-3.htm" :
                   if cond == 0 and st.getPlayer().isSubClassActive() :
                       if st.getPlayer().getLevel() >= 70 and count > 1:
                            htmltext=event
                            st.set("cond","0")
                            st.getPlayer().setNoble(True)
                            st.giveItems(NOBLESS_TIARA,1)
                            st.playSound("ItemSound.quest_finish")
                            st.exitQuest(1)
                            st.takeItems(GOLD_BAR,2)
                       else :
                            htmltext="31739-2.htm"
                            st.exitQuest(1)
                   else :
                       htmltext="31739-2.htm"
                       st.exitQuest(1)
               return htmltext

	def onTalk (self,npc,player):
	   htmltext = "<html><head><body>I have nothing to say you</body></html>"
           st = player.getQuestState(qn)
           if not st : return htmltext
           npcId = npc.getNpcId()
           id = st.getState()
           if id == State.CREATED :
               st.set("cond","0")
               htmltext="31739-1.htm"
           elif id == State.COMPLETED :
               htmltext = "<html><head><body>This quest have already been completed.</body></html>"
           else :
               st.exitQuest(1)
           return htmltext


QUEST = Quest(6666,qn,"custom")

for npcId in NPC:
 QUEST.addStartNpc(npcId)
 QUEST.addTalkId(npcId)
