# Written by Rolarga Version 0.3.1
import sys
from com.l2jmobius.gameserver.model import L2Attackable
from com.l2jmobius.gameserver.model.quest import State
from com.l2jmobius.gameserver.model.quest import QuestState
from com.l2jmobius.gameserver.model.quest.jython import QuestJython as JQuest

#Item declaration

# The Mark
MARK_OF_SUMMONER_ID = 3336

# Drops for Laras Parts, Lists, Arcanas & Crystals
LETOLIZARDMAN_AMULET_ID,   SAC_OF_REDSPORES_ID,       KARULBUGBEAR_TOTEM_ID,    \
SHARDS_OF_MANASHEN_ID,     BREKAORC_TOTEM_ID,         CRIMSON_BLOODSTONE_ID,    \
TALONS_OF_TYRANT_ID,       WINGS_OF_DRONEANT_ID,      TUSK_OF_WINDSUS_ID,       \
FANGS_OF_WYRM_ID,          LARS_LIST1_ID,             LARS_LIST2_ID,            \
LARS_LIST3_ID,             LARS_LIST4_ID,             LARS_LIST5_ID,            \
GALATEAS_LETTER_ID,        BEGINNERS_ARCANA_ID,       ALMORS_ARCANA_ID,         \
CAMONIELL_ARCANA_ID,       BELTHUS_ARCANA_ID,         BASILLIA_ARCANA_ID,       \
CELESTIEL_ARCANA_ID,       BRYNTHEA_ARCANA_ID,        CRYSTAL_OF_PROGRESS1_ID,  \
CRYSTAL_OF_INPROGRESS1_ID, CRYSTAL_OF_FOUL1_ID,       CRYSTAL_OF_DEFEAT1_ID,    \
CRYSTAL_OF_VICTORY1_ID,    CRYSTAL_OF_PROGRESS2_ID,   CRYSTAL_OF_INPROGRESS2_ID,\
CRYSTAL_OF_FOUL2_ID,       CRYSTAL_OF_DEFEAT2_ID,     CRYSTAL_OF_VICTORY2_ID,   \
CRYSTAL_OF_PROGRESS3_ID,   CRYSTAL_OF_INPROGRESS3_ID, CRYSTAL_OF_FOUL3_ID,      \
CRYSTAL_OF_DEFEAT3_ID,     CRYSTAL_OF_VICTORY3_ID,    CRYSTAL_OF_PROGRESS4_ID,  \
CRYSTAL_OF_INPROGRESS4_ID, CRYSTAL_OF_FOUL4_ID,       CRYSTAL_OF_DEFEAT4_ID,    \
CRYSTAL_OF_VICTORY4_ID,    CRYSTAL_OF_PROGRESS5_ID,   CRYSTAL_OF_INPROGRESS5_ID,\
CRYSTAL_OF_FOUL5_ID,       CRYSTAL_OF_DEFEAT5_ID,     CRYSTAL_OF_VICTORY5_ID,   \
CRYSTAL_OF_PROGRESS6_ID,   CRYSTAL_OF_INPROGRESS6_ID, CRYSTAL_OF_FOUL6_ID,      \
CRYSTAL_OF_DEFEAT6_ID,     CRYSTAL_OF_VICTORY6_ID = range(3337,3390)

# Lists and other Info-Stores

# any npcIds
NPC = [7063]+range(7634,7641)

# all stats
STATS = ["cond","step","Lara_Part","Arcanas","Beginner_Arcanas","Belthus","Brynthea","Celestiel","Camoniell","Basilla","Almors"]

# This stores any drop for Laras Parts, datas including mob npcs
# DROPLIST = [LaraPart,maxcount,chance,item]
DROPLIST_LARA = {
555: ["Lara_Part",1,80,SAC_OF_REDSPORES_ID],    # List 1
577: ["Lara_Part",1,25,LETOLIZARDMAN_AMULET_ID],
578: ["Lara_Part",1,25,LETOLIZARDMAN_AMULET_ID],
579: ["Lara_Part",1,25,LETOLIZARDMAN_AMULET_ID],
580: ["Lara_Part",1,50,LETOLIZARDMAN_AMULET_ID],
581: ["Lara_Part",1,75,LETOLIZARDMAN_AMULET_ID],
582: ["Lara_Part",1,75,LETOLIZARDMAN_AMULET_ID],
600: ["Lara_Part",2,80,KARULBUGBEAR_TOTEM_ID],  # List 2
563: ["Lara_Part",2,80,SHARDS_OF_MANASHEN_ID],
552: ["Lara_Part",3,60,CRIMSON_BLOODSTONE_ID],  # List 3
267: ["Lara_Part",3,25,BREKAORC_TOTEM_ID],
268: ["Lara_Part",3,25,BREKAORC_TOTEM_ID],
271: ["Lara_Part",3,25,BREKAORC_TOTEM_ID],
269: ["Lara_Part",3,50,BREKAORC_TOTEM_ID],
270: ["Lara_Part",3,50,BREKAORC_TOTEM_ID],
553: ["Lara_Part",4,70,TUSK_OF_WINDSUS_ID],     # List 4
192: ["Lara_Part",4,50,TALONS_OF_TYRANT_ID],
193: ["Lara_Part",4,50,TALONS_OF_TYRANT_ID],
89:  ["Lara_Part",5,30,WINGS_OF_DRONEANT_ID],   # List 5
90:  ["Lara_Part",5,60,WINGS_OF_DRONEANT_ID],
176: ["Lara_Part",5,50,FANGS_OF_WYRM_ID]
}

# This stores datas like required Items for lists and the random choice part
# ListNum = [listId, ItemId 1, ItemId 2]
LISTS = {
1: [LARS_LIST1_ID,SAC_OF_REDSPORES_ID,LETOLIZARDMAN_AMULET_ID], # List 1
2: [LARS_LIST2_ID,KARULBUGBEAR_TOTEM_ID,SHARDS_OF_MANASHEN_ID], # List 2
3: [LARS_LIST3_ID,CRIMSON_BLOODSTONE_ID,BREKAORC_TOTEM_ID],     # List 3
4: [LARS_LIST4_ID,TUSK_OF_WINDSUS_ID,TALONS_OF_TYRANT_ID],      # List 4
5: [LARS_LIST5_ID,WINGS_OF_DRONEANT_ID,FANGS_OF_WYRM_ID]        # List 5
}

# This stores all datas which are required for the Summoners in onTalk Part
SUMMONERS = {
7635: ["Almors",   ALMORS_ARCANA_ID,CRYSTAL_OF_VICTORY1_ID],   # Almors
7636: ["Camoniell",CAMONIELL_ARCANA_ID,CRYSTAL_OF_VICTORY2_ID],# Camoniell
7637: ["Belthus",  BELTHUS_ARCANA_ID,CRYSTAL_OF_VICTORY3_ID],  # Belthus
7638: ["Basilla",  BASILLIA_ARCANA_ID,CRYSTAL_OF_VICTORY4_ID], # Basilla
7639: ["Celestiel",CELESTIEL_ARCANA_ID,CRYSTAL_OF_VICTORY5_ID],# Celestiel
7640: ["Brynthea", BRYNTHEA_ARCANA_ID,CRYSTAL_OF_VICTORY6_ID]  # Brynthea
}

# This stores all datas for Summonkills/drops which are required in the onKill, onAttack and onDeath part
DROPLIST_SUMMON = {
5102: ["Almors",   CRYSTAL_OF_PROGRESS1_ID,CRYSTAL_OF_INPROGRESS1_ID,CRYSTAL_OF_FOUL1_ID,CRYSTAL_OF_DEFEAT1_ID,CRYSTAL_OF_VICTORY1_ID], # Pako the Cat
5103: ["Camoniell",CRYSTAL_OF_PROGRESS2_ID,CRYSTAL_OF_INPROGRESS2_ID,CRYSTAL_OF_FOUL2_ID,CRYSTAL_OF_DEFEAT2_ID,CRYSTAL_OF_VICTORY2_ID], # Mimi the Cat
5104: ["Belthus",  CRYSTAL_OF_PROGRESS3_ID,CRYSTAL_OF_INPROGRESS3_ID,CRYSTAL_OF_FOUL3_ID,CRYSTAL_OF_DEFEAT3_ID,CRYSTAL_OF_VICTORY3_ID], # Shadow Turen
5105: ["Basilla",  CRYSTAL_OF_PROGRESS4_ID,CRYSTAL_OF_INPROGRESS4_ID,CRYSTAL_OF_FOUL4_ID,CRYSTAL_OF_DEFEAT4_ID,CRYSTAL_OF_VICTORY4_ID], # Unicorn Racer
5106: ["Celestiel",CRYSTAL_OF_PROGRESS5_ID,CRYSTAL_OF_INPROGRESS5_ID,CRYSTAL_OF_FOUL5_ID,CRYSTAL_OF_DEFEAT5_ID,CRYSTAL_OF_VICTORY5_ID], # Unicorn Phantasm
5107: ["Brynthea", CRYSTAL_OF_PROGRESS6_ID,CRYSTAL_OF_INPROGRESS6_ID,CRYSTAL_OF_FOUL6_ID,CRYSTAL_OF_DEFEAT6_ID,CRYSTAL_OF_VICTORY6_ID]  # Silhoutte Tilfo
}

# Reduces the Beginner Arcanas on every try to kill a Summon by one, item and stat!
def takeBeginnerArcanas(st):
   st.takeItems(BEGINNERS_ARCANA_ID,1)
   st.set("Beginner_Arcanas",str(st.getInt("Beginner_Arcanas")-1))

class Quest (JQuest) :
   def __init__(self,id,name,descr):
      JQuest.__init__(self,id,name,descr)
      self.questItemIds = range(3337,3390)
      # list to hold the player and pet instance of the player in the duel and an "isFoul" flag, indexed by npcId
      self.inProgressDuelMobs = {}

   def onEvent (self,event,st) :
      htmltext = event
      if event == "7634-08.htm" :                    # start part for Galatea
         for var in STATS:
            if var in ["Arcanas","Beginner_Arcanas","Lara_Part"]:
               continue
            st.set(var,"1")
         st.setState(PROGRESS)
         st.playSound("ItemSound.quest_accept")
      elif event == "7634-07.htm" :
         if st.getQuestItemsCount(GALATEAS_LETTER_ID) == 0 :
            st.giveItems(GALATEAS_LETTER_ID,1)
      elif event == "7063-02.htm" :                  # Lara first time to give a list out
         random = st.getRandom(5)+1
         st.giveItems(LISTS[random][0],1)
         st.takeItems(GALATEAS_LETTER_ID,1)
         st.set("Lara_Part",str(random))
         st.set("step","2")
      elif event == "7063-04.htm" :                  # Lara later to give a list out
         random = st.getRandom(5)+1
         st.giveItems(LISTS[random][0],1)
         st.set("Lara_Part",str(random))
      elif event == "7635-02.htm" :                  # Almors' Part, this is the same just other items below.. so just one time comments
         if st.getInt("Beginner_Arcanas") :        # if the player has more then one beginners' arcana he can start a fight against the masters summon
            htmltext = "7635-03.htm"
            st.set("Almors","2")                     # set state ready to fight
      elif event == "7635-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS1_ID,1)     # give Starting Crystal
         st.takeItems(CRYSTAL_OF_FOUL1_ID,-1)        # just in case he cheated or loses
         st.takeItems(CRYSTAL_OF_DEFEAT1_ID,-1)
         takeBeginnerArcanas(st)                     # this takes one Beginner Arcana and set Beginner_Arcana stat -1
      elif event == "7636-02.htm" :                  # Camoniell's Part
         if st.getInt("Beginner_Arcanas") :
            htmltext = "7636-03.htm"
            st.set("Camoniell","2")
      elif event == "7636-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS2_ID,1)
         st.takeItems(CRYSTAL_OF_FOUL2_ID,-1)
         st.takeItems(CRYSTAL_OF_DEFEAT2_ID,-1)
         takeBeginnerArcanas(st)
      elif event == "7637-02.htm" :                  # Belthus' Part
         if st.getInt("Beginner_Arcanas") :
            htmltext = "7637-03.htm"
            st.set("Belthus","2")
      elif event == "7637-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS3_ID,1)
         st.takeItems(CRYSTAL_OF_FOUL3_ID,-1)
         st.takeItems(CRYSTAL_OF_DEFEAT3_ID,-1)
         takeBeginnerArcanas(st)
      elif event == "7638-02.htm" :                  # Basilla's Part
         if st.getInt("Beginner_Arcanas") :
            htmltext = "7638-03.htm"
            st.set("Basilla","2")
      elif event == "7638-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS4_ID,1)
         st.takeItems(CRYSTAL_OF_FOUL4_ID,-1)
         st.takeItems(CRYSTAL_OF_DEFEAT4_ID,-1)
         takeBeginnerArcanas(st)
      elif event == "7639-02.htm" :                  # Celestiel's Part
         if st.getInt("Beginner_Arcanas") :
            htmltext = "7639-03.htm"
            st.set("Celestiel","2")
      elif event == "7639-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS5_ID,1)
         st.takeItems(CRYSTAL_OF_FOUL5_ID,-1)
         st.takeItems(CRYSTAL_OF_DEFEAT5_ID,-1)
         takeBeginnerArcanas(st)
      elif event == "7640-02.htm" :                  # Brynthea's Part
         if st.getInt("Beginner_Arcanas") :
            htmltext = "7640-03.htm"
            st.set("Brynthea","2")
      elif event == "7640-04.htm" :
         st.giveItems(CRYSTAL_OF_PROGRESS6_ID,1)
         st.takeItems(CRYSTAL_OF_FOUL6_ID,-1)
         st.takeItems(CRYSTAL_OF_DEFEAT6_ID,-1)
         takeBeginnerArcanas(st)
      return htmltext

   def onTalk (self, npc, st):
      htmltext = "<html><body>I have nothing to say to you.</body></html>"
      id = st.getState()
      npcId = npc.getNpcId()
      Lara, Galatea, Almors, Camoniell, Belthus, Basilla, Celestiel, Brynthea = NPC
      if id == CREATED and npcId == Galatea:    # start part, Galatea
         for var in STATS:
            st.set(var,"0")
         if st.getPlayer().getClassId().getId() in [0x0b, 0x1a, 0x27]:
            if st.getPlayer().getLevel() > 38:  # conditions are ok, lets start
               htmltext = "7634-03.htm"
            else:
               htmltext = "7634-02.htm"         # too young.. not now
               st.exitQuest(1)
         else:                                  # wrong class.. never
            htmltext = "7634-01.htm"
            st.exitQuest(1)
      elif id == COMPLETED:                     # quest already done, not repeatable
         htmltext = "<html><body>This quest has already been completed.</body></html>"
      elif id == PROGRESS:
         step = st.getInt("step")               # stats as short vars if the player has state <Progress>
         LaraPart = st.getInt("Lara_Part")
         Arcanas = st.getInt("Arcanas")
         BeginnerArcanas = st.getInt("Beginner_Arcanas")
         if npcId == Galatea :            # Start and End Npc Galatea related stuff
            if step == 1 :                # step 1 means just started
               htmltext = "7634-09.htm"
            elif step == 2 :              # step 2 means already talkd with lara
               if Arcanas == 6:           # finished all battles... the player is able to earn the marks
                  htmltext = "7634-12.htm"
                  st.addExpAndSp(148409,30000)
                  for var in STATS:
                     st.unset(var)
                  st.setState(COMPLETED)
                  st.playSound("ItemSound.quest_finish")
                  st.giveItems(MARK_OF_SUMMONER_ID,1)
                  for item in [LARS_LIST1_ID,LARS_LIST2_ID,LARS_LIST3_ID,LARS_LIST4_ID,LARS_LIST5_ID,ALMORS_ARCANA_ID,BASILLIA_ARCANA_ID,CAMONIELL_ARCANA_ID,CELESTIEL_ARCANA_ID,BELTHUS_ARCANA_ID,BRYNTHEA_ARCANA_ID]:
                     st.takeItems(item,-1)
               else:                # he lost something :) or didnt finished
                  htmltext = "7634-10.htm"
         elif npcId == Lara:        # anything realated to Lara below
            if step == 1:           # first talk to lara
               htmltext = "7063-01.htm"
            else:                   # talk again to lara
               if LaraPart == 0:    # if you havent a part taken, give one
                  htmltext = "7063-03.htm"
               else:
                  ItemCount1 = st.getQuestItemsCount(LISTS[LaraPart][1])
                  ItemCount2 = st.getQuestItemsCount(LISTS[LaraPart][2])
                  if ItemCount1 < 30 or ItemCount2 < 30:   # if you have not enough materials, List 1 - 5
                     htmltext = "7063-05.htm"
                  elif ItemCount1 > 29 and ItemCount2 > 29:# if you have enough materials, receive your Beginner Arcanas, List 1 - 5
                     htmltext = "7063-06.htm"
                     st.giveItems(BEGINNERS_ARCANA_ID,2)
                     st.takeItems(LISTS[LaraPart][0],1)
                     st.takeItems(LISTS[LaraPart][1],-1)
                     st.takeItems(LISTS[LaraPart][2],-1)
                     st.set("Lara_Part","0")
                     st.set("Beginner_Arcanas",str(BeginnerArcanas+2))
         elif npcId in SUMMONERS.keys():              # just Summon Master related stuff
            SummonerStat = st.getInt(SUMMONERS[npcId][0])
            if step > 1:
               if SummonerStat == 1:            # default, just able to start talk with the summoner
                  htmltext = str(npcId)+"-01.htm"
               elif SummonerStat == 2:          # ready to fight... already take the mission to kill his pet
                  htmltext = str(npcId)+"-08.htm"
               elif SummonerStat == 3:          # in battle...
                  # this will add the player and his pet to the list of notified objects in onDeath Part
                  st.addNotifyOfDeath(st.getPlayer())
                  st.addNotifyOfDeath(st.getPlayer().getPet())
                  htmltext = str(npcId)+"-09.htm"
               elif SummonerStat == 4:          # haha... your summon lose
                  htmltext = str(npcId)+"-05.htm"
               elif SummonerStat == 5:          # hey.. shit cheater.. dont help your pet
                  htmltext = str(npcId)+"-06.htm"
               elif SummonerStat == 6:          # damn.. you won the batlle.. here are the arcanas
                  htmltext = str(npcId)+"-07.htm"
                  st.takeItems(SUMMONERS[npcId][2],-1) # take crystal of victory
                  st.giveItems(SUMMONERS[npcId][1],1)  # give arcana
                  st.set(SUMMONERS[npcId][0],"7")      # set 7, this mark that the players' summon won the battle
                  st.set("Arcanas",str(Arcanas+1))     # set arcana stat +1, if its 6... quest is finished and he can earn the mark
               elif SummonerStat == 7:                 # you already won the battle against my summon
                  htmltext = str(npcId)+"-10.htm"
      return htmltext

   def onDeath(self, killer, deadPerson, st) :               # if players summon dies, the crystal of defeat is given to the player and set stat to lose
      if not isinstance(killer, L2Attackable): return
      npcId = killer.getNpcId()
      if npcId in DROPLIST_SUMMON.keys() :
        # var means the variable of the SummonerManager, the rest are all Crystals which mark the status
        var,start,progress,foul,defeat,victory = DROPLIST_SUMMON[npcId]
        if st.getInt(var) == 3 :
           st.set(var,"4")
           st.giveItems(defeat,1)
      return

   # on the first attack, the stat is in battle... anytime gives crystal and set stat
   def onAttack (self, npc, player, damage, isPet):
      npcId = npc.getNpcId()
      st = player.getQuestState("230_TestOfSummoner")
      if npcId in DROPLIST_SUMMON.keys() :
         var,start,progress,foul,defeat,victory = DROPLIST_SUMMON[npcId]
         # check if this npc has been attacked before
         if self.inProgressDuelMobs.has_key(npcId) :
            if self.inProgressDuelMobs[npcId][2] : # if a foul already occured, skip all other checks
                return
            # check if the attacker is the same pet as the one that attacked before.
            # if not, mark this as a foul.
            if not isPet :
               self.inProgressDuelMobs[npcId][2] = True
            elif player.getPet() != self.inProgressDuelMobs[npcId][1] :
               self.inProgressDuelMobs[npcId][2] = True
         # if the npc had never before been attacked, check if it's time to mark a duel in progress
         elif not st :
            return
         elif st.getState() != PROGRESS :
            return
         elif not isPet and st.getInt(var) == 2:
            self.inProgressDuelMobs[npcId] = [player, player.getPet(), True] # foul
         else :
            # var means the variable of the SummonerManager, the rest are all Crystals which mark the status
            if st.getInt(var) == 2:
               st.set(var,"3")
               st.giveItems(progress,1)
               st.takeItems(start,1)
               st.playSound("Itemsound.quest_itemget")
               self.inProgressDuelMobs[npcId] = [player, player.getPet(), False] #mark the attack
      return

   def onKill (self, npc, player, isPet):
      npcId = npc.getNpcId()
      st = player.getQuestState("230_TestOfSummoner")
      if npcId in DROPLIST_LARA.keys() :
         if not st : return
         if st.getState() == COMPLETED : return
         random = st.getRandom(100)
         var, value, chance, item = DROPLIST_LARA[npcId]
         count = st.getQuestItemsCount(item)
         if st.getInt(var) == value and count < 30 and random < chance:
            st.giveItems(item,1)
            if count == 29:
               st.playSound("Itemsound.quest_middle")
            else:
               st.playSound("Itemsound.quest_itemget")
      # Part for npc summon death (duels part).  Some of this must run for all players.
      else :
         var,start,progress,foul,defeat,victory = DROPLIST_SUMMON[npcId]
         # 1-hit kill and bad synch may make onKill run before onAttack, having no previous attacker
         # If the attacker is the pet of a player who is doing the quest, mark it as a valid hit.
         if not self.inProgressDuelMobs.has_key(npcId) and isPet and st :
            if st.getInt(var) == 2:
               self.inProgressDuelMobs[npcId] = [player, player.getPet(), False]
         # if the killed mob is now in the progress list, there is work to be done...
         if self.inProgressDuelMobs.has_key(npcId) :
            # check if the attacker is the same pet as the one that attacked before.
            # if not, mark this as a foul.
            if not isPet :
               self.inProgressDuelMobs[npcId][2] = True
            elif player.getPet() != self.inProgressDuelMobs[npcId][1] :
               self.inProgressDuelMobs[npcId][2] = True
            # if a foul has NOT occured, give the player the victory crystal
            if not self.inProgressDuelMobs[npcId][2] :
               # var means the variable of the SummonerManager, the rest are all Crystals which mark the status
               var,start,progress,foul,defeat,victory = DROPLIST_SUMMON[npcId]
               if st.getInt(var) == 3 :
                  isName = 1     # first entry in the droplist is a name (string).  Skip it.
                  for item in DROPLIST_SUMMON[npcId] :        # take all crystal of this summoner away from the player
                     if isName != 1:
                         st.takeItems(item,-1)
                     isName = 0
                  st.set(var,"6")
                  st.giveItems(victory,1)       # if he wons without cheating, set stat won and give victory crystal
                  st.playSound("Itemsound.quest_middle")
            # if a foul has occured, find the player who had the duel in progress and give a foul crystal
            else :
               foulPlayer = self.inProgressDuelMobs[npcId][0]
               if foulPlayer :  # if not null (perhaps the player went offline)...
                  st = foulPlayer.getQuestState("230_TestOfSummoner")
                  if st :  # the original player has not aborted the quest
                     var,start,progress,foul,defeat,victory = DROPLIST_SUMMON[npcId]
                     if st.getInt(var) == 3:
                        st.set(var,"5")               # if the player cheats, give foul crystal and set stat to cheat
                        st.giveItems(foul,1)
            # finally, clear the inProgress mob info.
            self.inProgressDuelMobs.pop(npcId)
      return

QUEST          = Quest(230,"230_TestOfSummoner","Test Of Summoner")
CREATED        = State('Start', QUEST)
PROGRESS       = State('Progress', QUEST)
COMPLETED      = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(NPC[1])

# adds all npcs, mobs to the progress state
for npcId in NPC:
   QUEST.addTalkId(npcId)
for mobId in DROPLIST_LARA.keys():
   QUEST.addKillId(mobId)
for mobId in DROPLIST_SUMMON.keys():
   QUEST.addKillId(mobId)
   QUEST.addAttackId(mobId)