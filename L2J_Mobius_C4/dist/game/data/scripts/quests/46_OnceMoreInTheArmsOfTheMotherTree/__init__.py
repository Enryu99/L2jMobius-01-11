# Created by CubicVirtuoso
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

TRADER_GALLADUCCI_ID = 7097
GALLADUCCIS_ORDER_DOCUMENT_ID_1 = 7563
GALLADUCCIS_ORDER_DOCUMENT_ID_2 = 7564
GALLADUCCIS_ORDER_DOCUMENT_ID_3 = 7565
MAGIC_TRADER_GENTLER_ID = 7094
MAGIC_SWORD_HILT_ID = 7568
JEWELER_SANDRA_ID = 7090
GEMSTONE_POWDER_ID = 7567
PRIEST_DUSTIN_ID = 7116
PURIFIED_MAGIC_NECKLACE_ID = 7566
MARK_OF_TRAVELER_ID = 7570
SCROLL_OF_ESCAPE_SPECIAL = 7555
ADENA_ID = 57
RACE = 1

class Quest (JQuest) :

    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)
        self.questItemIds = [GALLADUCCIS_ORDER_DOCUMENT_ID_1, GALLADUCCIS_ORDER_DOCUMENT_ID_2, GALLADUCCIS_ORDER_DOCUMENT_ID_3, MAGIC_SWORD_HILT_ID, GEMSTONE_POWDER_ID, PURIFIED_MAGIC_NECKLACE_ID]

    def onEvent (self,event,st) :
        htmltext = event
        if event == "1" :
            st.set("cond","1")
            st.setState(STARTED)
            st.playSound("ItemSound.quest_accept")
            st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_1,1)
            htmltext = "7097-03.htm"
        elif event == "2" :
            st.set("cond","2")
            st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_1,1)
            st.giveItems(MAGIC_SWORD_HILT_ID,1)
            htmltext = "7094-02.htm"
        elif event == "3" :
            st.set("cond","3")
            st.takeItems(MAGIC_SWORD_HILT_ID,1)
            st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_2,1)
            htmltext = "7097-06.htm"
        elif event == "4" :
            st.set("cond","4")
            st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_2,1)
            st.giveItems(GEMSTONE_POWDER_ID,1)
            htmltext = "7090-02.htm"
        elif event == "5" :
            st.set("cond","5")
            st.takeItems(GEMSTONE_POWDER_ID,1)
            st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_3,1)
            htmltext = "7097-09.htm"
        elif event == "6" :
            st.set("cond","6")
            st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_3,1)
            st.giveItems(PURIFIED_MAGIC_NECKLACE_ID,1)
            htmltext = "7116-02.htm"
        elif event == "7" :
            st.giveItems(SCROLL_OF_ESCAPE_SPECIAL,1)
            st.takeItems(PURIFIED_MAGIC_NECKLACE_ID,1)
            htmltext = "7097-12.htm"
            st.set("cond","0")
            st.setState(COMPLETED)
            st.playSound("ItemSound.quest_finish")
        return htmltext

    def onTalk (Self,npc,st):
        npcId = npc.getNpcId()
        htmltext = "<html><body>I have nothing to say to you.</body></html>"
        id = st.getState()
        if id == CREATED :
            st.set("cond","0")
            if st.getPlayer().getRace().ordinal() == RACE and st.getQuestItemsCount(MARK_OF_TRAVELER_ID) > 0:
                htmltext = "7097-02.htm"
            else :
                htmltext = "7097-01.htm"
                st.exitQuest(1)
        elif npcId == 7097 and id == COMPLETED :
            htmltext = "<html><body>I can't supply you with another Scroll of Escape. Sorry traveller.</body></html>"
        elif npcId == 7097 and st.getInt("cond")==1 :
            htmltext = "7097-04.htm"
        elif npcId == 7097 and st.getInt("cond")==2 :
            htmltext = "7097-05.htm"
        elif npcId == 7097 and st.getInt("cond")==3 :
            htmltext = "7097-07.htm"
        elif npcId == 7097 and st.getInt("cond")==4 :
            htmltext = "7097-08.htm"
        elif npcId == 7097 and st.getInt("cond")==5 :
            htmltext = "7097-10.htm"
        elif npcId == 7097 and st.getInt("cond")==6 :
            htmltext = "7097-11.htm"
        elif npcId == 7094 and st.getInt("cond")==1 :
            htmltext = "7094-01.htm"
        elif npcId == 7094 and st.getInt("cond")==2 :
            htmltext = "7094-03.htm"
        elif npcId == 7090 and st.getInt("cond")==3 :
            htmltext = "7090-01.htm"
        elif npcId == 7090 and st.getInt("cond")==4 :
            htmltext = "7090-03.htm"
        elif npcId == 7116 and st.getInt("cond")==5 :
            htmltext = "7116-01.htm"
        elif npcId == 7116 and st.getInt("cond")==6 :
            htmltext = "7116-03.htm"
        return htmltext

QUEST       = Quest(46,"46_OnceMoreInTheArmsOfTheMotherTree","Once More In The Arms Of The Mother Tree")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7097)

QUEST.addTalkId(7097)
QUEST.addTalkId(7094)
QUEST.addTalkId(7090)
QUEST.addTalkId(7116)