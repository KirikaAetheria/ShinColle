package com.lulan.shincolle.utility;

import java.util.Comparator;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;

/** some targeting class/method
 */
public class TargetHelper {
	
	
	public TargetHelper() {}
	
	/**SORTER CLASS
	 * sort by distance (item 0 = nearest)
     */
    public static class Sorter implements Comparator {
        private final Entity targetEntity;

        public Sorter(Entity entity) {
            this.targetEntity = entity;
        }
        
        @Override
		public int compare(Object target1, Object target2) {
            return this.compare((Entity)target1, (Entity)target2);
        }

        //�t�ȷ|�Ʀblist�e��, �ȶV�j�V�᭱, list(0)�|�O�Z���̪񪺥ؼ�
        public int compare(Entity target1, Entity target2) {
            double d0 = this.targetEntity.getDistanceSqToEntity(target1);
            double d1 = this.targetEntity.getDistanceSqToEntity(target2);
            return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
        }       
    }//end sorter class
    
    /**TARGET SELECTOR
     * select target by class
     */
    public static class Selector implements IEntitySelector {
    	protected Entity host;
    	protected boolean isPVP;
    	
    	public Selector(Entity host) {
    		this.host = host;
    		
    		//PVP mode for ship
    		if(host instanceof BasicEntityShip) {
    			this.isPVP = ((BasicEntityShip) host).getStateFlag(ID.F.PVPFirst);
    		}
    		else {
    			this.isPVP = false;
    		}
    	}

		@Override
		public boolean isEntityApplicable(Entity target2) {
			//not self
			if(this.host == null || host.equals(target2)) {
				return false;
			}
			
			//ship host should check onSight
			if(host instanceof BasicEntityShip) {
    			if(((BasicEntityShip)host).getStateFlag(ID.F.OnSightChase)) {
        			if(!((BasicEntityShip)host).getEntitySenses().canSee(target2)) {
        				return false;
        			}
        		}
    		}
			//�Dship��host check onSight
    		if(host instanceof EntityLiving) {
    			if(!((EntityLiving)host).getEntitySenses().canSee(target2)) {
    				return false;
    			}
    		}
			
			//check ship and special entity for BasicEntityShip
			if(this.isPVP && (target2 instanceof BasicEntityShip || target2 instanceof BasicEntityAirplane ||
			   target2 instanceof BasicEntityMount || target2 instanceof EntityAbyssMissile)) {
				//must in banned list, NO ALLY or NEUTRAL target
				if(!EntityHelper.checkIsBanned(host, target2)) {
					return false;
				}
				
				//check alive
				if(target2.isEntityAlive() && !target2.isInvisible()) {
					return true;
				}//is alive
				
				return false;
			}
			
        	if((target2 instanceof EntityMob || target2 instanceof EntitySlime) &&
        	   target2.isEntityAlive() && !target2.isInvisible()) {
        		return true;
        	}//end is target class
        	else {  //custom class for mod interact
        		if(target2 != null && target2.isEntityAlive() && !target2.isInvisible() &&
        		   host instanceof IShipAttackBase) {
        			int pid = ((IShipAttackBase) host).getPlayerUID();
        			List<String> tarList = ServerProxy.getPlayerTargetClassList(pid);
        			String tarClass = target2.getClass().getSimpleName();
        			
        			if(tarList != null) {
        				for(String s : tarList) {
            				if(s.equals(tarClass)) {  //target class is in list
            					//if tameable entity, check owner
            					if(target2 instanceof IEntityOwnable) {
            						if(!EntityHelper.checkSameOwner(host, target2)) {
            							return true;
            						}
            					}
            					else {
            						return true;
            					}
            				}
            			}
        			}
        		}
        	}
        	return false;
        }
    }
    
    /** REVENGE TARGET SELECTOR */
    public static class RevengeSelector implements IEntitySelector {
    	protected Entity host;
    	
    	public RevengeSelector(Entity host) {
    		this.host = host;
    	}

		@Override
		public boolean isEntityApplicable(Entity target2) {
			//not self
			if(this.host == null || host.equals(target2)) {
				return false;
			}
			
			//check ship target
			if(target2 instanceof BasicEntityShip || target2 instanceof BasicEntityAirplane ||
			   target2 instanceof BasicEntityMount || target2 instanceof EntityAbyssMissile) {
				//do not attack ally
				if(EntityHelper.checkIsAlly(host, target2)) {
					return false;
				}
				
				//check alive
				if(target2.isEntityAlive() && !target2.isInvisible()) {
					return true;
				}
			}
			
			//check mob target
        	if((target2 instanceof EntityMob || target2 instanceof EntitySlime) &&
        	   target2.isEntityAlive() && !target2.isInvisible()) {
        		return true;
        	}//end is target class
        	else {  //custom class for mod interact
        		if(target2 != null && target2.isEntityAlive() && !target2.isInvisible() &&
        		   host instanceof IShipAttackBase) {
        			int pid = ((IShipAttackBase) host).getPlayerUID();
        			List<String> tarList = ServerProxy.getPlayerTargetClassList(pid);
        			String tarClass = target2.getClass().getSimpleName();
        			
        			if(tarList != null) {
        				for(String s : tarList) {
            				if(s.equals(tarClass)) {  //target class is in list
            					//if tameable entity, check owner
            					if(target2 instanceof EntityTameable) {
            						if(!EntityHelper.checkSameOwner(host, target2)) {
            							return true;
            						}
            					}
            					else {
            						return true;
            					}
            				}
            			}
        			}
        		}
        	}
        	
        	return false;
        }
    }
    
    /** TARGET SELECTOR FOR HOSTILE
     *  select target by class for hostile mob
     */
    public static class SelectorForHostile implements IEntitySelector {
    	private final Entity host;
    	
    	public SelectorForHostile(Entity host) {
    		this.host = host;
    	}
    	
    	@Override
		public boolean isEntityApplicable(Entity target2) {
        	if((target2 instanceof EntityPlayer || target2 instanceof BasicEntityShip ||
        	   target2 instanceof BasicEntityAirplane || target2 instanceof BasicEntityMount) && 
        	   target2.isEntityAlive() && !target2.isInvisible()) {
        		
        		//do not attack OP player
        		if(target2 instanceof EntityPlayer) {
        			return !EntityHelper.checkOP((EntityPlayer) target2);
        		}
        		
        		return true;
        	}
        	return false;
        }

    }
    
    /** REVENGE SELECTOR FOR HOSTILE
     *  select target by class for hostile mob
     */
    public static class RevengeSelectorForHostile implements IEntitySelector {
    	private final Entity host;
    	
    	public RevengeSelectorForHostile(Entity host) {
    		this.host = host;
    	}
    	
    	@Override
		public boolean isEntityApplicable(Entity target2) {
        	if(target2.isEntityAlive() && !target2.isInvisible()) {
        		//do not attack OP player
        		if(target2 instanceof EntityPlayer) {
        			return !EntityHelper.checkOP((EntityPlayer) target2);
        		}
        		
        		return true;
        	}
        	return false;
        }

    }

}