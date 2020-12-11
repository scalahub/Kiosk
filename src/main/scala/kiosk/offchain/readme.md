# Tx Builder

#### What is it?

- A tool for developing the offchain part of an Ergo dApp
- Enables one to create a transaction by specifying a **script** in Json. 
- Built on top of Kiosk with an example implementation in [KioskWallet](../wallet/KioskWallet.scala#L95-L138).  
- Can be used for many existing dApps, such as *Oracle-pools*, *Timestamping* and *Auctions*.

#### How does it work?

- Tx Builder allows us to define the input and data inputs of a transaction, along with some auxiliary boxes that are neither inputs nor data-inputs.  Auxiliary boxes are used only during computation.
- Each such box is defined using a **box definition**. A box definition is a sequence of **instructions** to filter boxes. Currently, we can filter using tokens, registers and nanoErgs. 
- Currently, boxes can be searched either by address or by box-id. With box-id, there can be at most one box, so we can unambiguously define a box. 
  However, when matching with address, there can be multiple boxes. These are handled as follows: 
  - The boxes are first filtered using the instructions. 
  - The resulting boxes are then sorted by value in decreasing order
  - The first box (if any) is selected as the matched box.
- An error is thrown if no boxes match a definition.


Tx Builder is more verbose than, for example, Scala. As an example, the Scala code `c = a + b` must be written in Tx Builder as
`{"name":"c", "first":"a", "op":"Add", "second":"b"}`.
That said, the only thing needed to use Tx Builder is the ability to write Json (and possibly use a pen and paper).

#### Protocol

The highest level of abstraction in Tx Builder is a [**Protocol**](compiler/model/package.scala#L10-L24).
A **Protocol** is made up of the following items: 
- Optional sequence of `Constant` declarations, using which we can encode arbitrary values into the script.
- Optional sequence of box definitions, `auxInputs`. 
  These are for accessing arbitrary boxes without having to use them as data inputs (or inputs).
- Optional sequence of box definitions, `dataInputs`, defining data-inputs of the transaction.
- Mandatory sequence of box definitions, `inputs`, defining inputs of the transaction. 
- Mandatory sequence of box definitions, `outputs`, defining outputs of the transaction.
- Optional sequence of `Unary` operations, used to convert between same types (example `Long` to `Long`).
- Optional sequence of `Binary` operations, used to compose two objects into a third object (of same types).
- Optional sequence of `Conversion` operations, used to convert between types (example `Address` to `ErgoTree`).
- Optional sequence of `Branch` instructions, used for run-time control-flow.

#### Declarations

The next level of abstraction is a [**Declaration**](compiler/Declaration.scala), which maps to an instance of a [`Kiosktype[_]`](../ergo.scala#L29-37).
For instance, an **Id** declaration maps to a `KioskCollByte` object (of size 32), 
while an **Address** declaration maps to a `KioskErgoTree` object.

We can classify declarations into three types:
- **Constants**: These specify initial values. Examples:  
  - `{"name":"myInt", "type":"int", "value":"123"}`
  - `{"name":"myAddress", "type":"address", "value":"9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"}`
- **Instructions**: These specify binary and unary operations and conversions. Examples:
  - Binary Op: `{"name":"mySum", "first":"someValue", "op":"Add", "second":"someOtherValue"}`
  - Unary Op: `{"out":"myNegativeNumber", "in":"myNumber", "op":"Neg"}`
  - Conversion: `{"to":"myErgoTree", "from":"myGroupElement", "converter":"ProveDlog"}`
- **Box declarations**: These are used to define or search for boxes. There are four types: **Address**, **Id**, **Register**, and **Long** (see below).  

See [this page](compiler/model/package.scala) for the detailed schema of all declarations and [this page](compiler/model/Enums.scala) for the enumerations used.

#### Box Declarations
There are four type of box declarations:
- **Address**: The address of the box, internally stored as `KioskErgoTree`.
- **Id**: Box Id or token Id, internally stored as a `KioskCollByte` of size 32.
- **Register**: Register contents, internally stored as `KioskType[_]`.
- **Long**: NanoErgs or token quantity, internally stored as `KioskLong`.

#### Names and Values
A box declaration can contain exactly one of:
- A `name` field (i.e., the declaration defines a new variable that will be referenced elsewhere), or
- A `value` field (i.e., the declaration references another variable that is already defined elsewhere).

The exception to this rule is the [**Long**](compiler/model/package.scala#L89-L103) declaration, which can have both fields, 
provided that it also has a third field `filter` present. A [`filter`](compiler/model/Enums.scala#L16) can be any of `Ge, Le, Gt, Lt, Ne`. 
Thus, a **Long** allows both of the following possibilities: 
1. Either `name` or `value` as in other declarations.
2. All of `name`, `value` and `filter`.   

The following are some example declarations:
1. `{"name":"myAddress"}`
2. `{"value":"myAddress"}`
3. `{"name":"actualNanoErgs", "value":"someMinValue", "filter":"Ge"}`

- The first defines the address `myAddress`.
- The second references that address.
- The third defines the (Long) value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs < someMinValue`. 

#### Targets and Pointers

For clarity, we use the following terminology when describing box declarations:
- A declaration that defines a variable is a "target".
- A declaration that references a variable is a "pointer".

We can then rewrite the rules for box declarations as follows:
- It can be either a target or a pointer but not both, with **Long** being the exception.
- An input can contain both pointers and targets.
- An output can only contain pointers.

The following rules apply for pointers and targets in an input:
- A pointer is a "search filter", i.e., used to fetch boxes from the blockchain.
For example, in `"boxId":{"value":"myBoxId"}`, the value contained in `myBoxId` (of type `KioskCollByte`) 
 will be used for fetching a box with that id.
- A target maps to some data in a box that has already been fetched from the blockchain.
For example, in `"address":{"name":"myAddress"}`, the address of the box will be stored in a variable called `myAddress`.

#### Input rules
The following rules apply for each input:
- It must have at least one of `boxId` or `address` declarations defined.
- If both `boxId` and `address` declarations have been defined, then both cannot be targets or pointers at the same time.

#### Token rules
A [**Token**](compiler/model/package.scala#L105-L109) is defined as 
`case class Token(index: Option[Int], id: Option[Id], amount: Option[Long])`. 
The main rule to follow here is that if `index` is empty then `id` must be defined, and that too as a pointer (i.e., it must have a `value` field). 
This is because the token index must be somehow determinable (either via an explicit `index` field or by matching the tokenId of a pointer.)

To illustrate this, the following are some valid token definitions:
1. `{"index":0, "id":{"name":"myTokenId"}, "amount":{"value":"otherTokenAmount"}}`.
   - Matches the token at index `0` if the amount is same as that of pointer `otherTokenAmount`. 
   - Creates a new target called `myTokenId` with the matched tokenId.
2. `{"index":0, "id":{"name":"myTokenId"}, "amount":{"name":"myTokenAmount"}}`. 
   - Matches the token at index `0`
   - Creates a new target called `myTokenId` containing the matched tokenId.
   - Creates a new target called `myTokenAmount` containing the matched token amount.
3. `{"id":{"value":"otherTokenId"}`. 
   - Matches the token at some index if the tokenId is same as that of pointer `otherTokenId`.
4. `{"id":{"value":"otherTokenId"}, "amount":{"value":"otherTokenAmount"}}}`. 
   - Matches the token at some index if both conditions hold:
     - The tokenId is the same as that of pointer `otherTokenId`. 
     - The amount is the same as that of `otherTokenAmount`.
5. `{"id":{"value":"otherTokenId"}, "amount":{"value":"otherTokenAmount", "filter":"Ge"}}`. 
   - Matches the token at some index if both conditions hold:
     - The tokenId is the same as that of pointer `otherTokenId`. 
     - The amount is >= the value returned by `otherTokenAmount`.
6. `{"id":{"value":"otherTokenId"}, "amount":{"name":"myTokenAmount"}}`. 
   - Matches the token at some index if the tokenId is the same as that of pointer `otherTokenId`. 
   - Creates a new target called `myTokenAmount` containing the matched token amount.

The following is an invalid token definition:
- `{"id":{"name":"myTokenId"}, "amount":{"name":"myTokenAmount"}}`. 

This is because if `id` is a target (i.e., has a `name` field) then `index` must be defined.

#### Strict token matching 

To ensure that the matched input has exactly those tokens defined in the search criteria and nothing more, use the `Strict` flag for that input definition:

```json
"inputs": [ 
  { 
    "address": { ... },
    "tokens": [ ... ],
    "registers": [ ... ],
    "options": ["Strict"]
  }
]
```

For instance, to select a box with no tokens, skip `tokens` field (or set it to empty array) and add the `Strict` option. 

This option applies to tokens only.

#### Matching multiple addresses in one definition

An [`Address`](compiler/model/package.scala#L51-L64) declaration takes an optional sequence, `values`, 
using which we can map one box definition to one of many addresses. 
As an example, in the oracle-pool the pool box addresses oscillate between *Live-epoch* and *Epoch-preparation*.
We can match such boxes as follows:

```json
"address": {
  "values": [
    "epochPreparationAddress",
    "liveEpochAddress"
  ]
}
```

A `values` field must have at least two elements. If we need to match a single address, we must instead use the `value` field as in other declarations.

We can use `name` to capture the actual matched address when multiple addresses are supplied via values `values`:

```json
"address": {
  "name": "actualPoolAddress",
  "values": [
    "epochPreparationAddress",
    "liveEpochAddress"
  ]
}
```

#### Order of evaluation
Declarations are evaluated in the following order:
- Constants
- Computation boxes (`boxes`)  (from low to high index)
- Data-input boxes (from low to high index) 
- Input boxes (from low to high index) 
- Output boxes
- Binary Ops, Unary Ops and Conversions are "Lazy" (i.e., evaluated only if needed)

#### Referencing rules
- The order of evaluation determines what can and cannot be referenced. A pointer can only refer to a target that has been evaluated previously. 
  - Thus, a pointer in inputs can refer to a target in data-inputs, but a pointer in data-inputs cannot refer to a target in inputs.
  - Similarly, a pointer in the second input can refer to a target in the first input, but a pointer in the first input cannot refer to a target in the second input.
- It is not possible for a pointer to refer to a target in the same input or data-input.
- As mentioned earlier, an output cannot contain targets. It can only contain pointers.

#### Complete example

The following is an example of a script to timestamp a box using the dApp described [here](https://www.ergoforum.org/t/a-trustless-timestamping-service-for-boxes/432/9?u=scalahub).
```JSON
{
  "constants": [
    {
      "name": "myBoxId",
      "type": "CollByte",
      "value": "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
    },
    {
      "name": "emissionAddress",
      "type": "Address",
      "value": "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
    },
    {
      "name": "timestampAddress",
      "type": "Address",
      "value": "4MQyMKvMbnCJG3aJ"
    },
    {
      "name": "myTokenId",
      "type": "CollByte",
      "value": "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
    },
    {
      "name": "minTokenAmount",
      "type": "Long",
      "value": "2"
    },
    {
      "name": "one",
      "type": "Long",
      "value": "1"
    },
    {
      "name": "minStorageRent",
      "type": "Long",
      "value": "2000000"
    }
  ],
  "dataInputs": [
    {
      "id": {
        "value": "myBoxId"
      }
    }
  ],
  "inputs": [
    {
      "address": {
        "value": "emissionAddress"
      },
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "name": "inputTokenAmount",
            "value": "minTokenAmount",
            "filter": "Ge"
          }
        }
      ],
      "nanoErgs": {
        "name": "inputNanoErgs"
      }
    }
  ],
  "outputs": [
    {
      "address": {
        "value": "emissionAddress"
      },
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "value": "balanceTokenAmount"
          }
        }
      ],
      "nanoErgs": {
        "value": "inputNanoErgs"
      }
    },
    {
      "address": {
        "value": "timestampAddress"
      },
      "registers": [
        {
          "value": "myBoxId",
          "num": "R4",
          "type": "CollByte"
        },
        {
          "value": "HEIGHT",
          "num": "R5",
          "type": "Int"
        }
      ],
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "value": "one"
          }
        }
      ],
      "nanoErgs": {
        "value": "minStorageRent"
      }
    }
  ],
  "binaryOps": [
    {
      "name": "balanceTokenAmount",
      "first": "inputTokenAmount",
      "op": "Sub",
      "second": "one"
    }
  ]
}
```

#### Development Status

Tx Builder is in *experimental* status. Please use it at your own risk and definitely read the source before using it.

Features in development (in order of priority):  
1. Currently, each input (and data-input) definition matches at most one on-chain box. We would prefer one input definition to match multiple boxes. We call them **multi-inputs** definitions.
2. There should be a meaningful way of mixing single and multi-input targets and pointers.
3. Allow literals to be directly used in declarations instead of via constants. Example: `"address":{"literal":"4MQyMKvMbnCJG3aJ"}`.
#### Using Tx Builder in your own wallet

Tx Builder is written in Scala, and therefore supports any JVM language. The following shows how to use it from Scala.
First include Kiosk in your project by doing the following in `build.sbt`:
```Scala
lazy val Kiosk = RootProject(uri("git://github.com/scalahub/Kiosk.git"))
lazy val root = (project in file(".")).dependsOn(Kiosk)
```

Then use it in your code by importing classes in the package `kiosk.offchain` and its sub-packages. Please refer to [KioskWallet.scala](../wallet/KioskWallet.scala#L85-L113) for details on how to use Tx Assembler to generate your own wallet transaction.
The following snippet (taken from KioskWallet) shows the main steps. 
```Scala
def txBuilder(script: String) = {
  val compileResults = compiler.Compiler.compile(Parser.parse(script))
  val feeNanoErgs = compileResults.fee.getOrElse(1000000L)
  val outputNanoErgs = compileResults.outputs.map(_.value).sum + feeNanoErgs 
  val deficientNanoErgs = (outputNanoErgs - compileResults.inputNanoErgs).max(0)

  val moreInputBoxIds = if (deficientNanoErgs > 0) {
    val myBoxes: Seq[ergo.KioskBox] = Explorer.getUnspentBoxes(myAddress).filterNot(compileResults.inputBoxIds.contains).sortBy(-_.value)
    boxSelector(deficientNanoErgs, myBoxes)
  } else Nil

  val inputBoxIds = compileResults.inputBoxIds ++ moreInputBoxIds
  // now we have all the information needed to create tx

  // the following is specific to KioskWallet. This is where your wallet code will come instead  
  Client.usingClient { implicit ctx =>    
    val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
    val dataInputBoxes: Array[InputBox] = ctx.getBoxesById(compileResults.dataInputBoxIds: _*)

    $ergoBox.$createTx(
      inputBoxes = inputBoxes,
      dataInputs = dataInputBoxes,
      boxesToCreate = compileResults.outputs.toArray,
      fee = feeNanoErgs,
      changeAddress = myAddress,
      proveDlogSecrets = Array(secretKey.toString(10)),
      dhtData = Array[DhtData](),
      broadcast = true
    ).toJson(false)
  }
}
```

