### ---------------------------------------------------------------------------
### <history>
###		Elektra@ElektraL2.com
### </history>
### ---------------------------------------------------------------------------

### Settings
NPC         = [8042,8043]
QuestId     = 3995
QuestName   = "echo"
QuestDesc   = "custom"
InitialHtml = "1.htm"

### Items - Format [name, giveItemId, giveItemQty, takeItem1Id, takeItem1Qty, takeItem2Id, takeItem2Qty]
Items       = [
["Theme of Travel",      4411, 1, 57, 200, 4410, 1],
["Theme of Festival",    4415, 1, 57, 200, 4421, 1],
["Theme of Lonely",      4414, 1, 57, 200, 4420, 1],
["Theme of Love",        4413, 1, 57, 200, 4408, 1],
["Theme of Battle",      4412, 1, 57, 200, 4409, 1],
["Theme of Comedy",      4417, 1, 57, 200, 4419, 1],
["Theme of Celebration", 4416, 1, 57, 200, 4418, 1]
]

### ---------------------------------------------------------------------------
### DO NOT MODIFY BELOW THIS LINE
### ---------------------------------------------------------------------------

import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

### doRequestedEvent
def do_RequestedEvent(event, st, giveItemId, giveItemQty, takeItem1Id, takeItem1Qty, takeItem2Id, takeItem2Qty) :
    if st.getQuestItemsCount(takeItem1Id) >= takeItem1Qty and st.getQuestItemsCount(takeItem2Id) >= takeItem2Qty :
        st.takeItems(takeItem1Id, takeItem1Qty)
        st.giveItems(giveItemId, giveItemQty)
        return "Echo Crystal Created"
    else :
        return "You do not have enough items."

### main code
class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "0" :
        return InitialHtml
    for item in Items :
        if event == str(item[1]):
            htmltext = do_RequestedEvent(event, st, item[1], item[2], item[3], item[4], item[5], item[6])
    if htmltext != event :
      st.setState(COMPLETED)
      st.exitQuest(1)
    return htmltext

 def onTalk (Self,npc,st):
   htmltext = "<html><body>I have nothing to say with you</body></html>"
   return InitialHtml

### Quest class and state definition
QUEST       = Quest(QuestId,str(QuestId) + "_" + QuestName,QuestDesc)
CREATED     = State('Start',     QUEST)
COMPLETED   = State('Completed', QUEST)

### Quest initialization
QUEST.setInitialState(CREATED)

for item in NPC:
### Quest NPC starter initialization
   QUEST.addStartNpc(item)
### Quest NPC initialization
   QUEST.addTalkId(item)