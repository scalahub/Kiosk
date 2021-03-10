## v0.7 (ERG-USD)

#### Differences with v0.6 (and 0.5)

- Different datapoint box contract (removed check for live epoch script)
- Same update NFT and ballot token id
- Different pool NFT and participant token id


v1 = min 1 oracle needed
v2 = min 2 oracles needed
v3 = min 4 oracles needed and different update NFT

### Test cases

1. Bootstrap epoch prep box (v1)
2. Move to live epoch stage (v1)
3. Move to epoch prep stage (v1)
   
4. Update to epoch prep box (v2)
5. Move to live epoch stage (v2)
6. Move to epoch prep stage (v2)
   
7. Update to epoch prep box (v3)
8. Move to live epoch stage (v3)
9. Move to epoch prep stage (v3)
   
10. Update to epoch prep box (v1)
11. Move to live epoch stage (v1)
12. Move to epoch prep stage (v1)

Basically the test should be that it is always possible to come to epoch prep stage of any version starting from anywhere