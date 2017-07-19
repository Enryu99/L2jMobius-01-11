# Stolen Dignity version 0.1 
# by DrLecter
import sys
from com.l2jmobius import Config
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

#Quest info
QUEST_NUMBER,QUEST_NAME,QUEST_DESCRIPTION = 386,"StolenDignity","Stolen Dignity"

#Variables
DROP_RATE=15*Config.RATE_DROP_QUEST
REQUIRED_ORE=100 #how many items will be paid for a game (affects onkill sounds too)

#Quest items
SI_ORE = 6363

#Rewards
REWARDS=[5529]+range(5532,5540)+range(5541,5549)
 
#Messages
default   = "<html><body>I have nothing to say to you.</body></html>"
error_1   = "Low_level.htm"
start     = "Start.htm"
starting  = "Starting.htm"
starting2 = "Starting2.htm"
binfo1    = "Bingo_howto.htm"
bingo     = "Bingo_start.htm"
bingo0    = "Bingo_starting.htm"
ext_msg   = "Quest aborted"

#NPCs
WK_ROMP = 7843

#Mobs
MOBS = [ 670,671,954,956,958,959,960,964,969,967,970,971,974,975,1001,1003,1005,1020,1021,1089,1108,1110,1113,1114,1116 ]

#templates
number  = ["second","third","fourth","fifth","sixth"]
header  = "<html><body>Warehouse Freightman Romp:<br><br>"
link    = "<td align=center><a action=\"bypass -h Quest 386_StolenDignity "
middle  = "</tr></table><br><br>Your selection thus far: <br><br><table border=1 width=120 hieght=64>"
footer  = "</table></body></html>"
loser   = "Wow! How unlucky can you get? Your choices are highlighted in red below. As you can see, your choices didn't make a single line! Losing this badly is actually quite rare!<br><br>You look so sad, I feel bad for you... Wait here...<br><br>.<br><br>.<br><br>.<br><br>Take this... I hope it will bring you better luck in the future.<br><br>"
winner  = "Excellent! As you can see, you've formed three lines! Congratulations! As promised, I'll give you some unclaimed merchandise from the warehouse. Wait here...<br><br>.<br><br>.<br><br>.<br><br>Whew, it's dusty! OK, here you go. Do you like it?<br><br>"
average = "Hum. Well, your choices are highlighted in red below. As you can see your choices didn't formed three lines... but you were near, so don't be sad. You can always get another few infernium ores and try again. Better luck in the future!<br><br>"

def partial(st) :
    html = " number:<br><br><table border=0><tr>"
    for z in range(1,10) :
        html += link+str(z)+"\">"+str(z)+"</a></td>"
    html += middle
    chosen = st.get("chosen").split()
    for y in range(0,7,3) :
        html +="<tr>"
        for x in range(3) :
	    html+="<td align=center>"+chosen[x+y]+"</td>"
        html +="</tr>"
    html += footer
    return html

def result(st) :
    chosen = st.get("chosen").split()
    grid = st.get("grid").split()
    html = "<table border=1 width=120 height=64>"
    for y in range(0,7,3) :
        html +="<tr>"
        for x in range(3) :
            html+="<td align=center>"
            if grid[x+y] == chosen[x+y] :
                html+="<font color=\"FF0000\"> "+grid[x+y]+" </font>"
            else :
                html+=grid[x+y]
            html+="</td>"
        html +="</tr>"
    html += footer
    return html


class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "yes" :
       htmltext = starting
       st.setState(STARTED)
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
    elif event == "binfo" :
        htmltext = binfo1
    elif event == "0" :
       htmltext = ext_msg
       st.exitQuest(1)
    elif event == "bingo" :
       if st.getQuestItemsCount(SI_ORE) >= REQUIRED_ORE :
         st.takeItems(SI_ORE,REQUIRED_ORE)
         htmltext = bingo0
         grid = range(1,10) #random.sample(xrange(1,10),9) ... damn jython that makes me think that inefficient stuff
         for i in range(len(grid)-1, 0, -1) :
           j = st.getRandom(8)
           grid[i], grid[j] = grid[j], grid[i]
         for i in range(len(grid)): grid[i]=str(grid[i])
         st.set("chosen","? ? ? ? ? ? ? ? ?")
         st.set("grid"," ".join(grid))
         st.set("playing","1")
       else :
         htmltext = "You don't have the items required."
    else :
       for i in range(1,10) :
          if event == str(i) :
            if st.getInt("playing"):
              chosen = st.get("chosen").split()
              grid = st.get("grid").split()
              if chosen.count("?") >= 3 :
                  chosen[grid.index(str(i))]=str(i)
                  st.set("chosen"," ".join(chosen))
                  if chosen.count("?")==3 :
                      htmltext = header
                      row = col = diag = 0
                      for i in range(3) :
                          if ''.join(chosen[3*i:3*i+3]).isdigit() : row += 1
                          if ''.join(chosen[i:9:3]).isdigit() : col += 1
                      if ''.join(chosen[0:9:4]).isdigit() : diag += 1
                      if ''.join(chosen[2:7:2]).isdigit() : diag += 1
                      if col == 1 and row == 1 and diag == 1 :
                          htmltext += winner
                          st.giveItems(REWARDS[st.getRandom(len(REWARDS))],4)
                          st.playSound("ItemSound.quest_finish")
                      elif diag == 0 and row == 0 and col == 0 :
                          htmltext += loser
                          st.giveItems(REWARDS[st.getRandom(len(REWARDS))],10)
                          st.playSound("ItemSound.quest_jackpot")
                      else :
                          htmltext += average
                          st.playSound("ItemSound.quest_giveup")
                      htmltext += result(st)
                      for var in ["chosen","grid","playing"]:
                          st.unset(var)
#                      st.exitQuest(1)
                  else :
                      htmltext = header+"Select your "+number[8-chosen.count("?")]+partial(st)
            else:
              htmltext=default
    return htmltext

 def onTalk (self,npc,st):
   htmltext = default
   id = st.getState()
   if id == CREATED :
      st.set("cond","0")
      if st.getPlayer().getLevel() < 58 :
         st.exitQuest(1)
         htmltext = error_1
      else :
         htmltext = start
   elif id == STARTED :
      if st.getQuestItemsCount(SI_ORE) >= REQUIRED_ORE :
         htmltext = bingo
      else :
         htmltext = starting2 
   return htmltext

 def onKill (self,npc,player,isPet) :
   partyMember = self.getRandomPartyMemberState(player, STARTED)
   if not partyMember : return
   st = partyMember.getQuestState(str(QUEST_NUMBER)+"_"+QUEST_NAME)
   if st :
     count = st.getQuestItemsCount(SI_ORE)
     if st.getRandom(100) < DROP_RATE :
        st.giveItems(SI_ORE,1)
        if (count + 1) % REQUIRED_ORE == 0 :
           st.playSound("ItemSound.quest_middle")
        else :
           st.playSound("ItemSound.quest_itemget")
   return  

# Quest class and state definition
QUEST       = Quest(QUEST_NUMBER, str(QUEST_NUMBER)+"_"+QUEST_NAME, QUEST_DESCRIPTION)
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)

QUEST.setInitialState(CREATED)
# Quest NPC starter initialization
QUEST.addStartNpc(WK_ROMP)
# Quest initialization
QUEST.addTalkId(WK_ROMP)

for i in MOBS :
  QUEST.addKillId(i)