# DeFi-Pool: An Oracle Pool for Ergo 

This article describes an oracle pool architecture for Ergo smart contracts to use data consolidated from multiple oracles. 
The most common use of this design is to use the oracle pool as a more reliable source of off-chain data (such as exchanges rates)
 supplied by multiple oracles.
 
## Introduction
 
In order to design DeFi applications such as Decentralized Exchanges (DEX), we need a reliable source of off-chain data, which is usually
modeled using an oracle, an trustworthy entity regularly posting such data on the blockchain. 
Smart contracts can then reference the oracle box via a singleton token.

However, using a single oracle has some drawbacks such as having to trust a single source of data that may be susceptible to corruption, or 
relying on a source that may stop functioning in future.

An oracle pool helps to solve both problems by consolidating and averaging data points supplied from multiple oracles and allowing smart contracts 
to use a single value that only references the pool contract. Even if some of the oracles turn out to supply bad data 
(deviating beyond a certain threshold from the average), the pool can filter those out as long as the corrupt ones don't form a majority.

At a high level the requirement from the pool is:

1. It should publish a consolidated data-point that is the average of the published data-points of all "good" oracles (defined below)
2. That consolidated data-point should be updated as the good oracles publish new data-points.

Note that one of the requirements of an oracle is that its published data-point is derived from a real world source. 
Although, it is not possible to check on-chain if the data-point published by an oracle deviated from the real world, 
it is possible to manually check this later on. 
We leave this task to a management committee, who will be responsible for "black-listing" and penalizing an oracle that publishes a wrong data-point. 
On the other hand, we can do certain checks on-chain if two conditions are satisfied:

- A majority of the oracles are honest, and
- The real world sources of the honest oracles supply data-point that are "close" enough. 

We can then define a good oracle as follows:

1. The management committee has not black-listed it.
2. It publishes the data-point at or very close to its schedule irrespective of the fee involved.
3. At any time, there is exactly one data-point published by the oracle, called its "current" data-point.
4. Its current data-point is "close" to the average of the current data-points of all non-black-listed oracles.   

Using this criteria, even if an oracle's intentions are good but its real world source is bad then it is inadvertently classified bad.  

Additionally, we want to enforce the following policy for the oracles:

1. [P1] Oracles should be rewarded for posting a good data-point on their schedule.
2. [P2] Oracles should not be penalized for posting a bad data-point (far from the average). 
3. [P3] Oracles should be penalized for not posting on their schedule. 
 
This section describes a design of a pool that satisfies everything except P3.

## Epochs

Since rates and other off-chain data change over time, the pool's data supply is divided into epochs. 
Each epoch must last for at least a minimum time (defined using the number of blocks) and can potentially last longer.
For the purpose of this discussion, assume that this minimum time is 30 blocks which is roughly one hour. 

    block 0                                  40                              75
          |<------ epoch 1 (40 blocks) ------>|<---- epoch 2 (35 blocks) ---->|          
          ^                                   ^                               ^
          |                                   |                               |
          |                                   |                               |
    epoch 1 start                       epoch 2 start                   epoch 3 start 

A new epoch is started when a collector posts a consolidated data point created by combining 
the data points posted by oracles since the current epoch started. 

Since an epoch in our system can have a variable duration, we call our model the **variable epoch** design. 
## Boxes

The main box here is a **Pool box**, which contains the pool contract described [here](https://github.com/scalahub/Kiosk/blob/master/src/test/scala/kiosk/OraclePool.scala#L64:L84).
The pool box contains the data-point in R4 (the first free register), which is to be considered the "valid" data point for the current epoch.
The design of the pool contract ensures there is only one pool box at any time. The pool box keeps track of the epoch start time in the "creation height" field of R3.
Smart contracts can references this box via a singleton token, the **pool-token**, that this box contains in quantity 1.  

A new pool box can only be created by destroying the current pool box if the creation height of the current pool box is at least 30 blocks lower than the current height.

There are also **oracle boxes** that contain the actual data points that are to be consolidated. These are identified by another token, the **oracle-token** that such a box must have.
Every oracle box has one quantity of the oracle-token.

## Oracles

Each oracles will own an oracle box and will keep posting data on the blockchain on a regular basis. 
An oracle can spend its previously posted box to create a fresh data point.
At any time, an oracle has only one box unspent on the blockchain, which will be used as a data point during collection 
provided that the box is **fresh**, that is, was created within the last 30 blocks.

An oracle may post multiple times during any epoch; only the most recent data point will be considered. 
 
The following figure explains the complete protocol.

  
	   0     5         15       25          40           55       65         75
	   |                                     |
	   |<------- (epoch 1: 40 blocks) ------>|<---- (epoch 2: 35 blocks) ---->|        
       ^     ^         ^        ^            ^            ^        ^          ^
	   |     |         |        |            |            |        |          |
	   |  oracle1   oracle2  oracle3         |         oracle1  oracle2       |
	   |     |         |        |            |            |        |          |
	   |  (stale)   (fresh)  (fresh)         |         (fresh)  (fresh)       |
       |                                     |                                |
       |        |                            |   |                            |
	   |        |<-------- 30 blocks ------->|   |<-------- 30 blocks ------->|
       |        |    (valid oracle period)   |   |    (valid oracle period)   |
       |                                     |                                |
       |                                     |                                |
    bootstrap                            collection                       collection 
    epoch 1 start                        epoch 2 start                    epoch 3 start


## Stages

An pool box can be in any of the below stages:

1. Active: When the creation height of the box is less than or equal to 30 blocks below the actual height. It is not spendable in this stage.
2. Inactive: When the creation height of the box is more than 30 blocks below the actual height. It can only be spent when in this stage. 
   
If the pool box has less funds to pay the oracles or the collector then it cannot be spent and once it becomes inactive, it will continue to do so. 
In this case, someone can fund the pool box (preserving the other parameters, thereby keeping it inactive) and giving it the potential to become active again.
If there are any fresh oracle boxes, then someone can collect them and make the pool box active.  


## Using the Pool Box

A smart contract will use the pool box as follows:

	val poolBox = CONTEXT.dataInputs(0)
	val maxAgeDiff = 100 // I will accept pool box created within 100 blocks
	val validRate = poolBox.tokens(0)._1 == poolTokenId && 
			poolBox.creationHeight >= HEIGHT - maxAgeDiff
    // use rate from poolBox if validRate
      