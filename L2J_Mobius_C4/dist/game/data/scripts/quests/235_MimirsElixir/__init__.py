# Mimir's Elixir version 0.1 
# by Fulminus
import sys
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest
#Quest info
QUEST_NUMBER,QUEST_NAME,QUEST_DESCRIPTION = 235,"MimirsElixir","Mimir's Elixir"

DROP_RATE = 20

#prerequisites:
STAR_OF_DESTINY = 5011
MINLEVEL = 75

#Quest items
PURE_SILVER = 6320
TRUE_GOLD = 6321
SAGES_STONE = 6322
BLOOD_FIRE = 6318
MIMIRS_ELIXIR = 6319

SCROLL_ENCHANT_WEAPON_A = 729

#Messages
default   = "<html><body>I have nothing to say to you.</body></html>"
#NPCs
LADD,JOAN=7721,7718
#Mobs, cond, Drop
DROPLIST = {
965: [3,SAGES_STONE],   #Chimera Piece
1090: [6,BLOOD_FIRE]    #Bloody Guardian
}

class Quest (JQuest) :
 
 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)
 
 def onEvent (self,event,st) :
    if event == "1" :
        st.setState(PROGRESS)
        st.set("cond","1")
        htmltext = "7166-02a.htm"
    elif event == "7718_1" :
        st.set("cond","3")
        htmltext = "7718-01a.htm"
    return htmltext
 
 def onTalk (self,npc,st):
    htmltext = default
    id = st.getState()
    npcId = npc.getNpcId()
    cond = st.getInt("cond")
    if npcId == LADD :
        if id == CREATED :
            st.set("cond","0")
            if st.getPlayer().getLevel() < MINLEVEL :
                st.exitQuest(1)
                htmltext = "7166-01.htm"     #not qualified
            elif not st.getQuestItemsCount(STAR_OF_DESTINY) :
                st.exitQuest(1)
                htmltext = "7166-01a.htm"     #not qualified
            elif cond==0 :
                htmltext = "7166-02.htm"    # Successful start: Bring me Pure silver from Reagents quest
        elif id == COMPLETED :
            htmltext = "<html><body>This quest has already been completed.</body></html>"
        # was asked to get pure silver but has not done so yet.  Repeat: get pure silver
        elif cond==1 and not st.getQuestItemsCount(PURE_SILVER) :
            htmltext = "7166-03.htm"    # Bring me Pure silver from Reagents quest
        # got the pure silver and came back.  Ask for TrueGold.
        elif cond==1 and st.getQuestItemsCount(PURE_SILVER) :
            st.set("cond","2")
            htmltext = "7166-04.htm"    # Bring me True Gold from Joan
        elif 1<cond<5 :
            htmltext = "7166-05.htm"    # Where is my GOLD?!  Bring to me first.
        # got the true gold...look for Blood fire
        elif cond==5 :
            st.set("cond","6")
            htmltext = "7166-06.htm"    # find Blood Fire from "bloody guardians"
        # still looking for blood fire?
        elif cond==6 :
            htmltext = "7166-07.htm"    # find Blood Fire from "bloody guardians"
        # Ah, you got the blood fire!  Time to mix them up!
        elif cond==7 and st.getQuestItemsCount(PURE_SILVER) and st.getQuestItemsCount(TRUE_GOLD):
            htmltext = "7166-08.htm"     # what are you standing there for?  Go to the cauldron and mix them...
        # you idiot, how did you lose your quest items?
        elif cond==7 :   
            htmltext = "7166-09.htm"     # Well...you already know what to do...go get the 3 items...
            st.set("cond","3")          # start over...yay...
        # cond for this quest is set to 8 from Supplier or Reagents, when you create Mimir's Elixir.
        # Finally, all is done...time to learn how to use the Elixir...
        elif cond==8 :
            htmltext = "7166-10.htm"     # here's what you do...
            st.takeItems(MIMIRS_ELIXIR,-1)
            st.giveItems(SCROLL_ENCHANT_WEAPON_A,1)
            st.setState(COMPLETED)
            st.unset("cond")
    elif npcId == JOAN and id == PROGRESS :
       # first time talking to Joan: You ask for True Gold, she sends you for Sage's stone
        if cond==2 :
            htmltext = "7718-01.htm"      # You want True Gold?  Please get the sage's stone.  Kill Chimera!
        # Why are you back already?  You don't have the stone.
        elif cond==3 :
            htmltext = "7718-02.htm"     # you haven't gotten the sage's stone yet?
        # aha!  Here is the sage's stone!  Cool, now we can make true gold
        elif cond==4 :
            st.takeItems(SAGES_STONE,-1)
            st.giveItems(TRUE_GOLD,1)
            st.set("cond","5")
            htmltext = "7718-03.htm"     # here you go...take the gold.  Now go back to ladd.
        elif cond>=5 :
            htmltext = "7718-04.htm"     # Go back to ladd already!
    return htmltext
 
 def onKill (self,npc,player,isPet) :
     st = player.getQuestState(str(QUEST_NUMBER)+"_"+QUEST_NAME)
     if st :
        if st.getState() != PROGRESS : return
        npcId = npc.getNpcId()
        drop = st.getRandom(100)
        cond = st.getInt("cond")
        dropcond = DROPLIST[npcId][0]
        if drop < DROP_RATE and cond == dropcond :
           if st.getQuestItemsCount(DROPLIST[npcId][1]) == 0 :
              st.giveItems(DROPLIST[npcId][1],1)
              st.playSound("ItemSound.quest_itemget")
              st.set("cond",str(cond+1))
     return

# Quest class and state definition
QUEST       = Quest(QUEST_NUMBER, str(QUEST_NUMBER)+"_"+QUEST_NAME, QUEST_DESCRIPTION)

CREATED     = State('Start',     QUEST)
PROGRESS    = State('Progress',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
# Quest NPC starter initialization
QUEST.addStartNpc(LADD)
# Quest initialization
QUEST.addTalkId(LADD)
QUEST.addTalkId(JOAN)

for i in DROPLIST.keys():
  QUEST.addKillId(i)