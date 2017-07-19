# Made by Mr. Have fun! Version 0.2
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

SIDRAS_LETTER1_ID = 1262
BLANK_SHEET1_ID = 1263
BLOODY_RUNE1_ID = 1264
GARMIEL_BOOK_ID = 1265
PRAYER_OF_ADON_ID = 1266
PENITENTS_MARK_ID = 1267
ASHEN_BONES_ID = 1268
ANDARIEL_BOOK_ID = 1269
ORB_OF_ABYSS_ID = 1270

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = range(1262,1270)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1" :
        st.set("id","0")
        htmltext = "7330-06.htm"
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
        st.giveItems(SIDRAS_LETTER1_ID,1)
    elif event == "413_1" :
          if st.getPlayer().getLevel() >= 19 and st.getPlayer().getClassId().getId() == 0x26 and st.getQuestItemsCount(ORB_OF_ABYSS_ID) == 0 :
            htmltext = "7330-05.htm"
            return htmltext
          elif st.getPlayer().getClassId().getId() != 0x26 :
              if st.getPlayer().getClassId().getId() == 0x2a :
                htmltext = "7330-02a.htm"
              else:
                htmltext = "7330-03.htm"
          elif st.getPlayer().getLevel()<19 and st.getPlayer().getClassId().getId() == 0x26 :
              htmltext = "7330-02.htm"
          elif st.getPlayer().getLevel() >= 19 and st.getPlayer().getClassId().getId() == 0x26 and st.getQuestItemsCount(ORB_OF_ABYSS_ID) == 1 :
              htmltext = "7330-04.htm"
    elif event == "7377_1" :
          htmltext = "7377-02.htm"
          st.takeItems(SIDRAS_LETTER1_ID,1)
          st.giveItems(BLANK_SHEET1_ID,5)
          st.set("cond","2")
    elif event == "7375_1" :
          htmltext = "7375-02.htm"
    elif event == "7375_2" :
            htmltext = "7375-03.htm"
    elif event == "7375_3" :
            htmltext = "7375-04.htm"
            st.takeItems(PRAYER_OF_ADON_ID,1)
            st.giveItems(PENITENTS_MARK_ID,1)
            st.set("cond","5")
    return htmltext

 def onTalk (Self,npc,st):
   npcId = npc.getNpcId()
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   id = st.getState()
   if id == CREATED :
     st.setState(STARTING)
     st.set("cond","0")
     st.set("onlyone","0")
     st.set("id","0")
   if npcId == 7330 and st.getInt("cond")==0 :
        if st.getInt("cond")<15 :
          htmltext = "7330-01.htm"
        else:
          htmltext = "7330-01.htm"
   elif npcId == 7330 and st.getInt("cond") :
        if st.getQuestItemsCount(SIDRAS_LETTER1_ID) == 1 :
          htmltext = "7330-07.htm"
        elif st.getQuestItemsCount(BLANK_SHEET1_ID)>0 or st.getQuestItemsCount(BLOODY_RUNE1_ID) == 1 :
            htmltext = "7330-08.htm"
        elif st.getQuestItemsCount(ANDARIEL_BOOK_ID) == 0 and st.getQuestItemsCount(PRAYER_OF_ADON_ID)+st.getQuestItemsCount(GARMIEL_BOOK_ID)+st.getQuestItemsCount(PENITENTS_MARK_ID)+st.getQuestItemsCount(ASHEN_BONES_ID)>0 :
            htmltext = "7330-09.htm"
        elif st.getQuestItemsCount(ANDARIEL_BOOK_ID) == 1 and st.getQuestItemsCount(GARMIEL_BOOK_ID) == 1 :
            htmltext = "7330-10.htm"
            st.takeItems(ANDARIEL_BOOK_ID,1)
            st.takeItems(GARMIEL_BOOK_ID,1)
            st.giveItems(ORB_OF_ABYSS_ID,1)
            st.set("cond","0")
            st.setState(COMPLETED)
            st.playSound("ItemSound.quest_finish")
   elif npcId == 7377 and st.getInt("cond") :
        if st.getQuestItemsCount(SIDRAS_LETTER1_ID) == 1 :
          htmltext = "7377-01.htm"
        elif st.getQuestItemsCount(BLANK_SHEET1_ID) == 5 and st.getQuestItemsCount(BLOODY_RUNE1_ID) == 0 :
            htmltext = "7377-03.htm"
        elif st.getQuestItemsCount(BLOODY_RUNE1_ID)>0 and st.getQuestItemsCount(BLOODY_RUNE1_ID)<5 :
            htmltext = "7377-04.htm"
        elif st.getQuestItemsCount(BLOODY_RUNE1_ID) >= 5 :
            htmltext = "7377-05.htm"
            st.takeItems(BLOODY_RUNE1_ID,st.getQuestItemsCount(BLOODY_RUNE1_ID))
            st.giveItems(GARMIEL_BOOK_ID,1)
            st.giveItems(PRAYER_OF_ADON_ID,1)
            st.set("cond","4")
        elif st.getQuestItemsCount(PRAYER_OF_ADON_ID)+st.getQuestItemsCount(PENITENTS_MARK_ID)+st.getQuestItemsCount(ASHEN_BONES_ID)>0 :
            htmltext = "7377-06.htm"
        elif st.getQuestItemsCount(ANDARIEL_BOOK_ID) == 1 and st.getQuestItemsCount(GARMIEL_BOOK_ID) == 1 :
            htmltext = "7377-07.htm"
   elif npcId == 7375 and st.getInt("cond") :
      if st.getQuestItemsCount(PRAYER_OF_ADON_ID) == 1 :
        htmltext = "7375-01.htm"
      elif st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID) == 0 and st.getQuestItemsCount(ANDARIEL_BOOK_ID) == 0 :
          htmltext = "7375-05.htm"
      elif st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID)<10 and st.getQuestItemsCount(ASHEN_BONES_ID)>0 :
          htmltext = "7375-06.htm"
      elif st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID) >= 10 :
          htmltext = "7375-07.htm"
          st.takeItems(ASHEN_BONES_ID,st.getQuestItemsCount(ASHEN_BONES_ID))
          st.takeItems(PENITENTS_MARK_ID,st.getQuestItemsCount(PENITENTS_MARK_ID))
          st.giveItems(ANDARIEL_BOOK_ID,1)
          st.set("cond","7")
      elif st.getQuestItemsCount(ANDARIEL_BOOK_ID) == 1 :
          htmltext = "7375-08.htm"
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("413_PathToShillienOracle")
   if st :
      if st.getState() != STARTED : return
      npcId = npc.getNpcId()
      if npcId == 776 :
        st.set("id","0")
        if st.getInt("cond") and st.getQuestItemsCount(BLANK_SHEET1_ID)>0 :
          st.giveItems(BLOODY_RUNE1_ID,1)
          st.takeItems(BLANK_SHEET1_ID,1)
          if st.getQuestItemsCount(BLANK_SHEET1_ID) == 0 :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","3")
          else:
            st.playSound("ItemSound.quest_itemget")
      elif npcId == 514 :
        st.set("id","0")
        if st.getInt("cond") and st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID)<10 :
          st.giveItems(ASHEN_BONES_ID,1)
          if st.getQuestItemsCount(ASHEN_BONES_ID) == 10 :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","6")
          else:
            st.playSound("ItemSound.quest_itemget")
      elif npcId == 515 :
        st.set("id","0")
        if st.getInt("cond") and st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID)<10 :
          st.giveItems(ASHEN_BONES_ID,1)
          if st.getQuestItemsCount(ASHEN_BONES_ID) == 10 :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","6")
          else:
            st.playSound("ItemSound.quest_itemget")
      elif npcId == 457 :
        st.set("id","0")
        if st.getInt("cond") and st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID)<10 :
          st.giveItems(ASHEN_BONES_ID,1)
          if st.getQuestItemsCount(ASHEN_BONES_ID) == 10 :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","6")
          else:
            st.playSound("ItemSound.quest_itemget")
      elif npcId == 458 :
        st.set("id","0")
        if st.getInt("cond") and st.getQuestItemsCount(PENITENTS_MARK_ID) == 1 and st.getQuestItemsCount(ASHEN_BONES_ID)<10 :
          st.giveItems(ASHEN_BONES_ID,1)
          if st.getQuestItemsCount(ASHEN_BONES_ID) == 10 :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","6")
          else:
            st.playSound("ItemSound.quest_itemget")
   return

QUEST       = Quest(413,"413_PathToShillienOracle","Path To Shillien Oracle")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7330)

QUEST.addTalkId(7330)
QUEST.addTalkId(7375)
QUEST.addTalkId(7377)

QUEST.addKillId(457)
QUEST.addKillId(458)
QUEST.addKillId(514)
QUEST.addKillId(515)
QUEST.addKillId(776)