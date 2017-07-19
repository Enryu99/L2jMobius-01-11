# Made by Mr. Have fun! Version 0.2
# Updated by ElgarL
# Improved a lil' bit by DrLecter

import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

MARK_OF_PILGRIM_ID = 2721
BOOK_OF_SAGE_ID = 2722
VOUCHER_OF_TRIAL_ID = 2723
SPIRIT_OF_FLAME_ID = 2724
ESSENSE_OF_FLAME_ID = 2725
BOOK_OF_GERALD_ID = 2726
GREY_BADGE_ID = 2727
PICTURE_OF_NAHIR_ID = 2728
HAIR_OF_NAHIR_ID = 2729
STATUE_OF_EINHASAD_ID = 2730
BOOK_OF_DARKNESS_ID = 2731
DEBRIS_OF_WILLOW_ID = 2732
TAG_OF_RUMOR_ID = 2733
ADENA_ID = 57

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = range(2722,2734)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1" :
      htmltext = "7648-04.htm"
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
      st.giveItems(VOUCHER_OF_TRIAL_ID,1)
    elif event == "7648_1" :
          htmltext = "7648-05.htm"
    elif event == "7648_2" :
          htmltext = "7648-06.htm"
    elif event == "7648_3" :
          htmltext = "7648-07.htm"
    elif event == "7648_4" :
          htmltext = "7648-08.htm"
    elif event == "7648_5" :
          htmltext = "7648-05.htm"
    elif event == "7649_1" :
          htmltext = "7649-04.htm"
          st.giveItems(SPIRIT_OF_FLAME_ID,1)
          st.takeItems(ESSENSE_OF_FLAME_ID,1)
          st.set("cond","5")
    elif event == "7650_1" :
          if st.getQuestItemsCount(ADENA_ID) >= 100000*int(Config.RATE_DROP_ADENA) :
            htmltext = "7650-02.htm"
            st.giveItems(BOOK_OF_GERALD_ID,1)
            st.takeItems(ADENA_ID,100000*int(Config.RATE_DROP_ADENA))
            st.set("cond","7")
          else:
            htmltext = "7650-03.htm"
    elif event == "7650_2" :
          htmltext = "7650-03.htm"
    elif event == "7362_1" :
          htmltext = "7362-05.htm"
          st.takeItems(BOOK_OF_DARKNESS_ID,1)
          st.set("cond","16")
    elif event == "7362_2" :
          htmltext = "7362-04.htm"
          st.set("cond","16")
    elif event == "7652_1" :
          htmltext = "7652-02.htm"
          st.giveItems(BOOK_OF_DARKNESS_ID,1)
          st.takeItems(DEBRIS_OF_WILLOW_ID,1)
          st.set("cond","15")
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
   if npcId == 7648 and st.getInt("cond")==0 and st.getInt("onlyone")==0 :
        if (st.getPlayer().getClassId().getId() in [0x0f,0x1d,0x2a,0x32]) :
           if st.getPlayer().getLevel() >= 35 :
              htmltext = "7648-03.htm"
           else :
              htmltext = "7648-01.htm"
              st.exitQuest(1)
        else:
          htmltext = "7648-02.htm"
          st.exitQuest(1)
   elif npcId == 7648 and st.getInt("cond")==0 and st.getInt("onlyone")==1 :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 7648 and st.getInt("cond")==1 and st.getQuestItemsCount(VOUCHER_OF_TRIAL_ID) :
      htmltext = "7648-09.htm"
   elif npcId == 7648 and st.getInt("cond")==17 and st.getQuestItemsCount(BOOK_OF_SAGE_ID) :
      st.addExpAndSp(77832,16000)
      st.giveItems(7562,8)
      htmltext = "7648-10.htm"
      st.takeItems(BOOK_OF_SAGE_ID,1)
      st.giveItems(7562,8)
      st.giveItems(MARK_OF_PILGRIM_ID,1)
      st.setState(COMPLETED)
      st.playSound("ItemSound.quest_finish")
      st.set("onlyone","1")
      st.set("cond","0")
   elif npcId == 7571 and st.getInt("cond")==1 and st.getQuestItemsCount(VOUCHER_OF_TRIAL_ID) :
      htmltext = "7571-01.htm"
      st.takeItems(VOUCHER_OF_TRIAL_ID,1)
      st.set("cond","2")
   elif npcId == 7571 and st.getInt("cond")==2 :
      htmltext = "7571-02.htm"
   elif npcId == 7571 and st.getInt("cond")==5 and st.getQuestItemsCount(SPIRIT_OF_FLAME_ID) :
      htmltext = "7571-03.htm"
   elif npcId == 7649 and st.getInt("cond")==2 :
      htmltext = "7649-01.htm"
      st.set("cond","3")
   elif npcId == 7649 and st.getInt("cond")==3 :
      htmltext = "7649-02.htm"
   elif npcId == 7649 and st.getInt("cond")==4 and st.getQuestItemsCount(ESSENSE_OF_FLAME_ID) :
      htmltext = "7649-03.htm"
   elif npcId == 7550 and st.getInt("cond")==5 and st.getQuestItemsCount(SPIRIT_OF_FLAME_ID) :
      htmltext = "7550-01.htm"
      st.giveItems(TAG_OF_RUMOR_ID,1)
      st.set("cond","6")
   elif npcId == 7550 and st.getInt("cond")==6 :
      htmltext = "7550-02.htm"
   elif npcId == 7650 and st.getInt("cond")==6 and st.getQuestItemsCount(TAG_OF_RUMOR_ID) :
      htmltext = st.showHtmlFile("7650-01.htm").replace("RequiredAdena", str(100000*int(Config.RATE_DROP_ADENA)))
   elif npcId == 7650 and st.getInt("cond")>=8 and st.getQuestItemsCount(GREY_BADGE_ID) and st.getQuestItemsCount(BOOK_OF_GERALD_ID) :
      htmltext = "7650-04.htm"
      rate=int(Config.RATE_QUESTS_REWARD)
      if rate == 0 : rate = 1
      st.giveItems(ADENA_ID,int(100000*Config.RATE_DROP_ADENA/rate))
      st.takeItems(BOOK_OF_GERALD_ID,1)
   elif npcId == 7651 and st.getInt("cond")==6 and st.getQuestItemsCount(TAG_OF_RUMOR_ID) :
      htmltext = "7651-01.htm"
      st.giveItems(GREY_BADGE_ID,1)
      st.takeItems(TAG_OF_RUMOR_ID,1)
      st.set("cond","8")
   elif npcId == 7651 and st.getInt("cond")==7 and st.getQuestItemsCount(TAG_OF_RUMOR_ID) :
      htmltext = "7651-02.htm"
      st.giveItems(GREY_BADGE_ID,1)
      st.takeItems(TAG_OF_RUMOR_ID,1)
      st.set("cond","8")
   elif npcId == 7651 and st.getInt("cond")==8 :
      htmltext = "7651-03.htm"
   elif npcId == 7117 and st.getInt("cond")==8 :
      htmltext = "7117-01.htm"
      st.set("cond","9")
   elif npcId == 7117 and st.getInt("cond")==9 :
      htmltext = "7117-02.htm"
   elif npcId == 7036 and st.getInt("cond")==9 :
      htmltext = "7036-01.htm"
      st.giveItems(PICTURE_OF_NAHIR_ID,1)
      st.set("cond","10")
   elif npcId == 7036 and st.getInt("cond")==10 :
      htmltext = "7036-02.htm"
   elif npcId == 7036 and st.getInt("cond")==11 :
      htmltext = "7036-03.htm"
      st.giveItems(STATUE_OF_EINHASAD_ID,1)
      st.takeItems(PICTURE_OF_NAHIR_ID,1)
      st.takeItems(HAIR_OF_NAHIR_ID,1)
      st.set("cond","12")
   elif npcId == 7036 and st.getInt("cond")==12 and st.getQuestItemsCount(STATUE_OF_EINHASAD_ID) :
      htmltext = "7036-04.htm"
   elif npcId == 7362 and st.getInt("cond")==12 :
      htmltext = "7362-01.htm"
      st.set("cond","13")
   elif npcId == 7362 and st.getInt("cond")==13 :
      htmltext = "7362-02.htm"
   elif npcId == 7362 and st.getInt("cond")==15 and st.getQuestItemsCount(BOOK_OF_DARKNESS_ID) :
      htmltext = "7362-03.htm"
   elif npcId == 7362 and st.getInt("cond")==16 :
      htmltext = "7362-06.htm"
   elif npcId == 7362 and st.getInt("cond")==15 and st.getQuestItemsCount(BOOK_OF_DARKNESS_ID)==0 :
      htmltext = "7362-07.htm"
   elif npcId == 7652 and st.getInt("cond")==14 and st.getQuestItemsCount(DEBRIS_OF_WILLOW_ID) :
      htmltext = "7652-01.htm"
   elif npcId == 7652 and st.getInt("cond")==15 and st.getQuestItemsCount(BOOK_OF_DARKNESS_ID) :
      htmltext = "7652-03.htm"
   elif npcId == 7612 and st.getInt("cond")==16 :
      htmltext = "7612-01.htm"
      st.giveItems(BOOK_OF_SAGE_ID,1)
      if st.getQuestItemsCount(BOOK_OF_DARKNESS_ID) :
        st.takeItems(BOOK_OF_DARKNESS_ID,1)
      st.set("cond","17")
      st.takeItems(GREY_BADGE_ID,1)
      st.takeItems(SPIRIT_OF_FLAME_ID,1)
      st.takeItems(STATUE_OF_EINHASAD_ID,1)
   elif npcId == 7612 and st.getInt("cond")==17 :
      htmltext = "7612-02.htm"
   return htmltext

 def onKill (self,npc,player,isPet):
   st = player.getQuestState("215_TrialOfPilgrim")
   if st :
     if st.getState() != STARTED : return
     npcId = npc.getNpcId()
     if npcId == 5116 :
        if st.getInt("cond") and st.getInt("cond") == 3 and st.getQuestItemsCount(ESSENSE_OF_FLAME_ID) == 0 :
          if st.getRandom(5) == 1 :
            st.giveItems(ESSENSE_OF_FLAME_ID,1)
            st.set("cond","4")
            st.playSound("ItemSound.quest_middle")
     elif npcId == 5117 :
        if st.getInt("cond") and st.getInt("cond") == 10 and st.getQuestItemsCount(HAIR_OF_NAHIR_ID) == 0 :
          st.giveItems(HAIR_OF_NAHIR_ID,1)
          st.set("cond","11")
          st.playSound("ItemSound.quest_middle")
     elif npcId == 5118 :
        if st.getInt("cond") and st.getInt("cond") == 13 and st.getQuestItemsCount(DEBRIS_OF_WILLOW_ID) == 0 :
          if st.getRandom(5) == 1 :
            st.giveItems(DEBRIS_OF_WILLOW_ID,1)
            st.set("cond","14")
            st.playSound("ItemSound.quest_middle")
   return

QUEST       = Quest(215,"215_TrialOfPilgrim","Trial Of Pilgrim")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(7648)

QUEST.addTalkId(7036)
QUEST.addTalkId(7117)
QUEST.addTalkId(7362)
QUEST.addTalkId(7550)
QUEST.addTalkId(7571)
QUEST.addTalkId(7612)
QUEST.addTalkId(7648)
QUEST.addTalkId(7649)
QUEST.addTalkId(7650)
QUEST.addTalkId(7651)
QUEST.addTalkId(7652)

QUEST.addKillId(5116)
QUEST.addKillId(5117)
QUEST.addKillId(5118)