package com.HollowPlugins.HollowCreativePlots;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class DatabaseAccessManager {
	private HollowCreativePlots plugin;
	
	public DatabaseAccessManager(HollowCreativePlots _plugin) {
		plugin = _plugin;
	}
	
	public void printTables() {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%'";

                 statement = connection.prepareStatement(selectQuery);
     
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	
                	 plugin.log(resultSet.getString("name"));
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);

		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
	}
	
	public List<CreativePlot> getAllPlots(){
		List<CreativePlot> plots = new ArrayList<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM " + plugin.databaseTableName;

                 statement = connection.prepareStatement(selectQuery);
                 
     
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	 CreativePlot plot = readPlotFromResults(resultSet);
                	 if(plot!=null) plots.add(plot);
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);

		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return plots;
	}
	
	public List<CreativePlot> getPlayerPlots(String playerUUID){
		List<CreativePlot> plots = new ArrayList<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM "+plugin.databaseTableName+" WHERE "+plugin.plotOwnerUUIDColumn+" = ?";

                 statement = connection.prepareStatement(selectQuery,PreparedStatement.RETURN_GENERATED_KEYS);
                 statement.setString(1, playerUUID);
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	 CreativePlot plot = readPlotFromResults(resultSet);
                	 if(plot!=null) plots.add(plot);
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);
		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return plots;
	}
	
	public boolean addPlot(CreativePlot plot) {
		boolean addedPlot=false;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "INSERT INTO "+plugin.databaseTableName+" ("+plugin.plotOwnerUUIDColumn+","+plugin.creativeCornerColumn+","+plugin.diagonalVectorColumn+","
	        			 +plugin.overworldCornerColumn+","+plugin.creationDateColumn+") VALUES (?,?,?,?,?)"; 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);

                 statement.setString(1, plot.getOwnerUUID());
                 statement.setString(2, plot.getCreativedCorner());
                 statement.setString(3, plot.getSpan());
                 statement.setString(4, plot.getOverworldCorner());
                 statement.setString(5, plot.getCreationTime());

                 addedPlot =statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		return addedPlot;
	}
	
	
	public int getRowID(CreativePlot plot) {
		int rowID=-1;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getDatabase().getDatabaseConnection();
	         
	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM " + plugin.databaseTableName+" WHERE "+plugin.plotOwnerUUIDColumn+" = ? AND "+plugin.creationDateColumn+" = ?";
                 
                 statement = connection.prepareStatement(selectQuery);
                 statement.setString(1, plot.getOwnerUUID());
                 statement.setString(2,plot.getCreationTime());
                 
            	 resultSet = statement.executeQuery();
            	  
                 if (resultSet.next()) {
                	 rowID = resultSet.getInt("id");
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
	     } finally {
	         try {
	             if (resultSet != null) {
	            	 resultSet.close();
	             }
	             if (statement != null) {
	            	 statement.close();
	             }
	             if (connection != null) {
	                 connection.close();
	             }
	         } catch (SQLException e) {
	             plugin.log("Could not close resultsets, statements or connection, " + e);
	         }
	     }
		return rowID;
	}
	public boolean removePlot(CreativePlot plot) {
		boolean removedPlot=false;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "DELETE FROM "+plugin.databaseTableName+" WHERE id = ?";
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 statement.setInt(1, plot.getRowID());
	             
                 removedPlot=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return removedPlot;
	}
	
	private CreativePlot readPlotFromResults(ResultSet resultSet) {
		try {
			int rowID = resultSet.getInt("id");
			String uuidString = resultSet.getString(plugin.plotOwnerUUIDColumn);
			String bottomCornerString = resultSet.getString(plugin.creativeCornerColumn);
			String originalCornerString = resultSet.getString(plugin.overworldCornerColumn);
			String diagonalString = resultSet.getString(plugin.diagonalVectorColumn);
			String creationDateString = resultSet.getString(plugin.creationDateColumn);
 
 			UUID ownerUUID = UUID.fromString(uuidString);
			Vector creativeCornerVector = CreativePlot.deserializeVector(bottomCornerString);
			Vector overworldCornerVector = CreativePlot.deserializeVector(originalCornerString);
			Vector spanVector = CreativePlot.deserializeVector(diagonalString);
			LocalDateTime creationTime = LocalDateTime.parse(creationDateString);
 			CreativePlot plot = new CreativePlot(plugin,ownerUUID,overworldCornerVector,spanVector,creativeCornerVector,creationTime,rowID);
 			return plot;
		} catch (Exception e) {}
		return null;
	}
	
	private QueueEntry readTransferQueueEntryFromResults(ResultSet resultSet) {
		try {
			String sourceWorld = resultSet.getString(plugin.queueSourceWorld);
			String destinationWorld = resultSet.getString(plugin.queueDestinationWorld);
			String sourceCorner = resultSet.getString(plugin.queueSourceCorner);
			String destinationCorner = resultSet.getString(plugin.queueDestinationCorner);
 
			Vector sourceCornerVector = CreativePlot.deserializeVector(sourceCorner);
			Vector destinationCornerVector = CreativePlot.deserializeVector(destinationCorner);
			World source = Bukkit.getServer().getWorld(sourceWorld);
			World destination = Bukkit.getServer().getWorld(destinationWorld);

			if(source.equals(destination)) {
				return new CleanupChunkEntry(source,destination,sourceCornerVector,destinationCornerVector);
			} else {
				return new QueueEntry(source,destination,sourceCornerVector,destinationCornerVector);
			}
		} catch (Exception e) {}
		return null;
	}
	
	
	public List<QueueEntry> getTransferQueue(){
		List<QueueEntry> queue = new ArrayList<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM "+plugin.queueDatabaseName;

                 statement = connection.prepareStatement(selectQuery,PreparedStatement.RETURN_GENERATED_KEYS);
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	 QueueEntry entry = readTransferQueueEntryFromResults(resultSet);
                	 if(entry!=null) queue.add(entry);
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);
		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return queue;
	}
	
	public boolean addToTransferQueue(QueueEntry entry) {
		boolean addedPlot=false;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "INSERT INTO "+plugin.queueDatabaseName+" ("+plugin.queueSourceWorld+","+plugin.queueDestinationWorld+","+plugin.queueSourceCorner+","
	        			 +plugin.queueDestinationCorner+") VALUES (?,?,?,?)"; 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);

                 statement.setString(1, entry.sourceWorld.getName());
                 statement.setString(2, entry.destinationWorld.getName());
                 statement.setString(3, CreativePlot.serializeVector(entry.sourceCorner));
                 statement.setString(4, CreativePlot.serializeVector(entry.destinationCorner));

                 addedPlot =statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		return addedPlot;
	}
	
	public boolean addToTransferQueue(List<QueueEntry> entries) {
		boolean addedPlot=true;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
	        	 for(QueueEntry entry:entries) {
	        		 String insertQuery = "INSERT INTO "+plugin.queueDatabaseName+" ("+plugin.queueSourceWorld+","+plugin.queueDestinationWorld+","+plugin.queueSourceCorner+","
		        			 +plugin.queueDestinationCorner+") VALUES (?,?,?,?)"; 
	                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);

	                 statement.setString(1, entry.sourceWorld.getName());
	                 statement.setString(2, entry.destinationWorld.getName());
	                 statement.setString(3, CreativePlot.serializeVector(entry.sourceCorner));
	                 statement.setString(4, CreativePlot.serializeVector(entry.destinationCorner));

	                 addedPlot&=statement.executeUpdate()>0;
	                 statement.close();
	        	 }
	        	 
	        	 
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		return addedPlot;
	}
	
	public boolean removeFromTransferQueue(QueueEntry entry) {
		boolean removedPlot=false;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "DELETE FROM "+plugin.queueDatabaseName+" WHERE "+plugin.queueSourceWorld+" = ? AND "+plugin.queueDestinationWorld+" = ? AND "
	        			 +plugin.queueSourceCorner+" = ? AND "+plugin.queueDestinationCorner+" = ?";
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 
                 statement.setString(1, entry.sourceWorld.getName());
                 statement.setString(2, entry.destinationWorld.getName());
                 statement.setString(3, CreativePlot.serializeVector(entry.sourceCorner));
                 statement.setString(4, CreativePlot.serializeVector(entry.destinationCorner));
                 
                 removedPlot=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return removedPlot;
	}
	
	public boolean clearTransferQueue() {
		boolean removedPlot=false;
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "DELETE FROM "+plugin.queueDatabaseName;
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 
                 removedPlot=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return removedPlot;
	}
	
	private String serializeVector(Vector vect) {
		String vector = ""+vect.getBlockX()+","+vect.getBlockY()+","+vect.getBlockZ();
		return vector;
	}
	
	private Vector deserializeVector(String vect) {
		if(vect.split(",").length!=3) return null;
		try {
			int x = Integer.parseInt(vect.split(",")[0]);
			int y = Integer.parseInt(vect.split(",")[1]);
			int z = Integer.parseInt(vect.split(",")[2]);

			Vector vector = new Vector(x,y,z);
			return vector;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean putCreativeBlockChange(int projectID,Vector position,String blockData) {
		String serializedPosition = serializeVector(position);
		if(!updateCreativeBlock(projectID,serializedPosition,blockData)) {
			return insertCreativeBlock(projectID,serializedPosition,blockData);
		}
		return true;
	}
	
	private boolean updateCreativeBlock(int projectID,String position,String blockData) {
		boolean updatedBlock=false;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "UPDATE "+plugin.blockModificationTable+" SET "+plugin.coordinateColumn+" = ?, "+plugin.blockDataColumn+" = ? WHERE "+plugin.projectIDColumn+" = ?"
	        	 		+ " AND "+plugin.coordinateColumn+" = ?";
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 
                 statement.setString(1, position);
                 statement.setString(2, blockData);
                 statement.setInt(3, projectID);
                 statement.setString(4, position);

                 updatedBlock=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return updatedBlock;
	}
	
	private boolean insertCreativeBlock(int projectID,String position,String blockData) {
		boolean updatedBlock=false;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "INSERT INTO "+plugin.blockModificationTable+" ("+plugin.projectIDColumn+","+plugin.coordinateColumn+","+plugin.blockDataColumn+") VALUES (?,?,?)";
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 
                 
                 statement.setInt(1, projectID);
                 statement.setString(2, position);
                 statement.setString(3, blockData);
                 
                 
                 updatedBlock=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return updatedBlock;
	}
	
	public List<DatabaseBlockEntry> getProjectBlockChanges(int projectID){
		List<DatabaseBlockEntry> blocks = new ArrayList<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM "+plugin.blockModificationTable+" WHERE "+plugin.projectIDColumn+" = ?";

                 statement = connection.prepareStatement(selectQuery,PreparedStatement.RETURN_GENERATED_KEYS);
                 statement.setInt(1, projectID);
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	 try {
                		 Vector position = deserializeVector(resultSet.getString(plugin.coordinateColumn));
                		 String blockData = resultSet.getString(plugin.blockDataColumn);
                		 String rawBlock = blockData;
                		 String rawData = "";
                		 if(blockData.indexOf("[")>-1) {
                			 rawData = blockData.substring(blockData.indexOf("["));
                			 rawBlock = blockData.substring(0,blockData.indexOf("["));
                		 }
                		 BlockData data = null;
                		 try {data=Bukkit.createBlockData(blockData);} catch (Exception e) {};
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,false),rawData);
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,true),rawData);
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,false));
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,true));
                			 } catch (Exception e) {};
                		 }
                		 
                		 if(data==null) {
                			 plugin.log("Couldn't match block for: "+blockData);
                			 continue;
                		 }
                		 
                		 DatabaseBlockEntry entry = new DatabaseBlockEntry(projectID,position,data);
                		 blocks.add(entry);
                	 } catch(Exception e) {
                		 e.printStackTrace();
                	 }
                	 
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);
		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return blocks;	
	}
	
	
	public int cleanupOrphanedBlockEntries(Set<Integer> existingPlotIDs) {
		int count=0;
		List<DatabaseBlockEntry> allBlocks = getAllBlocks();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 
	        	 for(DatabaseBlockEntry entry:allBlocks) {
	     			if(!existingPlotIDs.contains(entry.projectID)) {
	     				String insertQuery = "DELETE FROM "+plugin.blockModificationTable+" WHERE "+plugin.projectIDColumn+" = ? AND "+plugin.coordinateColumn+" = ?";
	                    statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
	         			String position = serializeVector(entry.position);
	         			statement.setInt(1, entry.projectID);
	         			statement.setString(2, position);
	                    count+=statement.executeUpdate();
	     			}
	     		}
	        	 
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
	     }
			
		return count;
	}
	
	public int removeProjectBlockEntries(int plotID) {
		int count=0;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 
	        	
 				String insertQuery = "DELETE FROM "+plugin.blockModificationTable+" WHERE "+plugin.projectIDColumn+" = ?";
                statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
     			statement.setInt(1, plotID);
                count+=statement.executeUpdate();
	     	
	        	 
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
	     }
			
		return count;
	}
	
	private List<DatabaseBlockEntry> getAllBlocks(){
		List<DatabaseBlockEntry> blocks = new ArrayList<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();

	         if (connection != null && !connection.isClosed()) {
                 String selectQuery = "SELECT * FROM "+plugin.blockModificationTable;

                 statement = connection.prepareStatement(selectQuery,PreparedStatement.RETURN_GENERATED_KEYS);
            	 resultSet = statement.executeQuery();
            	  
                 while (resultSet.next()) {
                	 try {
                		 int projectID = resultSet.getInt(plugin.projectIDColumn);
                		 Vector position = deserializeVector(resultSet.getString(plugin.coordinateColumn));
                		 String blockData = resultSet.getString(plugin.blockDataColumn);
                		 String rawBlock = blockData;
                		 String rawData = "";
                		 if(blockData.indexOf("[")>-1) {
                			 rawData = blockData.substring(blockData.indexOf("["));
                			 rawBlock = blockData.substring(0,blockData.indexOf("["));
                		 }
                		 BlockData data = null;
                		 try {data=Bukkit.createBlockData(blockData);} catch (Exception e) {};
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,false),rawData);
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,true),rawData);
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,false));
                			 } catch (Exception e) {};
                		 }
                		 if(data==null) {
                    		 try {
                    			 data=Bukkit.createBlockData(Material.matchMaterial(rawBlock,true));
                			 } catch (Exception e) {};
                		 }
                		 
                		 if(data==null) {
                			 plugin.log("Couldn't match block for: "+blockData);
                			 continue;
                		 }
                		 
                		 DatabaseBlockEntry entry = new DatabaseBlockEntry(projectID,position,data);
                		 blocks.add(entry);
                	 } catch(Exception e) {
                		 e.printStackTrace();
                	 }
                	 
                 }
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
		     } catch (SQLException e) {
		    	 plugin.log("Error: "+e);
		     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return blocks;	
	}
	
	public boolean deleteCreativeBlock(DatabaseBlockEntry entry) {
		String position = serializeVector(entry.position);
		boolean updatedBlock=false;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
	         connection = plugin.getQueueDatabase().getDatabaseConnection();
	         if (connection != null && !connection.isClosed()) {
	        	 String insertQuery = "DELETE FROM "+plugin.blockModificationTable+" WHERE "+plugin.projectIDColumn+" = ? AND "+plugin.coordinateColumn+" = ?";
	        	 
                 statement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                 
                 
                 statement.setInt(1, entry.projectID);
                 statement.setString(2, position);
                 
                 updatedBlock=statement.executeUpdate()>0;
	         } else {
	             plugin.getLogger().log(Level.INFO,"Database connection was NULL or closed");
	         }
	     } catch (SQLException e) {
		    	 plugin.getLogger().log(Level.INFO,"Could not load players, " + e);
	     } finally {
		         try {
		             if (resultSet != null) {
		            	 resultSet.close();
		             }
		             if (statement != null) {
		            	 statement.close();
		             }
		             if (connection != null) {
		                 connection.close();
		             }
		         } catch (SQLException e) {
		             plugin.log("Could not close resultsets, statements or connection, " + e);
		         }
		     }
		
		return updatedBlock;
	}
	
}
