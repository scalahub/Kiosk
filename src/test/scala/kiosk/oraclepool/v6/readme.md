## v0.6 (ERG-USD)

#### Differences with v0.5

- Slightly different parameters (epoch length, buffer, reward amount)
- Will be updated via voting mechanism

## Casting a Vote for v0.6

Oracle pool operators holding the ballot token can cast the vote for 0.6.
The update will happen to the Epoch-Preparation box.

The updated address of this box is 
`2Qgfhy6m7AhxRU3xMHBXLtbXLWj6cs8cvSCsJdHh9rkZx64ZnzDDxBWBh5GsPkSuXcxfMGt36fLchZk2fMxXqxBzUcisq7HRo8kZA3Gp9fjqJYZAtuomUSkirWTEiM6X8kgahypcsRAvEt2PcXX2BLcJo1VfR7XggvhnNLCTqBxRDaWMVsERXVWVuoEU3DPNz67EVVnRdEqpuMUo7yoGZCSDufujyWHWszNmdCMEDBuF9WDEw3efnFMg8W81AYZJZcnAggfoZNtpjYx7stN26GRvW32m9x8AnW2Sz8gigg3rTyhavPFgwA2D1W59UQ1pTP2Dgb5kDT1yRi42Q5uimXaAbzGLqhz2tMmh1ds5X7N9LbKNnoyqe9N9agJ7GVeFBt6VUYAmfwuguWrgeA62kk2qerckhNdyBPjwSQmKMzomnwxkNZ5sKwgAAnjzMwfyJwbyueACgFe4dWomTSEXEDFBG4gqCTW2NhGw1p`

The hash of the ergo-tree corresponding to this address is `20ef45eef675d34d2347148b05346729328790d98d8b4ee95c11940b1d855666e1`

The ballot box has the following structure:
1. Address should be the oracle operators address
2. R4 will have the above hash value
3. R5 will have the box id of the update box
4. 1st token will be the ballot token.

The following is the ready-made request for the operators to make. They just need to add their node's address. 

```json
[
  {
    "address": "<put your address here>",
    "value": 1000000,
    "assets": [
      {
        "tokenId": "004b2ca8adbcf9f15c1149fab8264dbcafbdd3c784bb4fae7ee549c16774914b",
        "amount": 1
      }
    ],
    "registers": { 
       "R4": "0e20ef45eef675d34d2347148b05346729328790d98d8b4ee95c11940b1d855666e1",
       "R5": "0e200a5080e899273cb27b21de63ab243ef49150e6764118c50391723159e98fe3d4"
    }
  }
]
```

The request must be made to the endpoint `/wallet/payment/send` on the node (via swagger). See screenshot below:

![Screenshot from 2021-03-02 02-20-22](https://user-images.githubusercontent.com/23208922/109557622-59b13680-7afe-11eb-85fd-6badc66f5d50.png)




