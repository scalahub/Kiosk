# Tx Builder

Tx Builder is a tool for developing the offchain part of an Ergo DApp. It allows one to specify the offchain component of 
any Ergo application protocol in Json and build a transaction to participate in the protocol. It is to be used in conjunction with KioskWallet. However, it can also be used as a stand-alone library for a different wallet. 

Tx Builder is more verbose than, for example, Scala. As an example the Scala code `c = a + b` must be written in Tx Builder as 
`{"name":"c", "first":"a", "op":"Add", "second":"b"}`.
In the long-term, it may be better to develop a higher-level language on top of Tx Builder that is less verbose. 

#### Protocol

The highest level of abstraction in Tx Builder is a `Protocol`, 
which is a specification of the data-inputs, inputs and outputs of the transaction to be created.
A `Protocol` is made up of the following items: 
- Constants
- Data-input box definitions
- Input box definitions 
- Output box definitions
- Unary operations
- Binary operations
- Conversions

#### Declarations

The next level of abstraction is a [`Declaration`](compiler/Declaration.scala), which maps to an instance of a [`Kiosktype[_]`](../ergo.scala#L29-37).
For instance, an `Id` declaration maps to a `KioskCollByte` object (of size 32), 
while an `Address` declaration maps to a `KioskErgoTree` object.

We can classify declarations into three types:
- **Constants**: These specify initial values. Examples:  
  - `{"name":"myInt", "type":"int", "value":"123"}`
  - `{"name":"myAddress", "type":"address", "value":"9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"}`
- **Instructions**: These specify binary and unary operations and conversions. Examples:
  - Binary Op: `{"name":"mySum", "first":"someValue", "op":"Add", "second":"someOtherValue"}`
  - Unary Op: `{"out":"myNegativeNumber", "in":"myNumber", "op":"Neg"}`
  - Conversion: `{"to":"myErgoTree", "from":"myGroupElement", "converter":"ProveDlog"}`
- **Box declarations**: These are used to define boxes or search for boxes. There are four types: `Address`, `Id`, `Register`, and `Long` (see below).  

See [this page](model/package.scala) for the detailed schema of all declarations and [this page](model/Enums.scala) for the enumerations used.

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

The exception is the [`Long`](model/package.scala#L72-L81) declaration, which can have both fields. It can additionally have a [`filter`](model/Enums.scala#L16) field for inequalities. . 

The following are some example declarations:
1. `{"name":"myAddress"}`
2. `{"value":"myAddress"}`
3. `{"name":"actualNanoErgs", "value":"someMinValue", "filter":"Ge"}`
4. `{"name":"actualNanoErgs", "value":"someMinValue"}`

- The first defines the address `myAddress`.
- The second references that address.
- The third defines the value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs < someMinValue`. 
- The fourth defines the value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs != someMinValue`. (the default filter `Eq` is assumed here)

 
#### Target or Pointer?

For clarity, we use the following terminology when describing box declarations:
- A declaration that defines a variable is a "target".
- A declaration that reference a variable is a "pointer".

We can then rewrite the rules for box declarations as follows:
- It can be either a target or a pointer but not both, with `Long` being the exception.
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
- It must have at least one of `boxId` or `address` defined.
- If both `boxId` and `address` have been defined, then both cannot be targets or pointers at the same time.

#### Order of evaluation
Order of evaluation (resolution of variables):
- Constants
- Data-inputs (from low to high index) 
- Inputs (from low to high index) 
- Outputs
- Binary Ops, Unary Ops and Conversions are "Lazy" (i.e., evaluated only if needed)

#### Reference Rules
- The order of evaluation determines what can and cannot be referenced. A pointer can only refer to a target that has been evaluated previously. 
  - Thus, a pointer in inputs can refer to a target in data-inputs, but a pointer in data-inputs cannot refer to a target in inputs.
  - Similarly, a pointer in the second input can refer to a target in the first input, but a pointer in the first input cannot refer to a target in the second input.
- It is not possible for a pointer to refer to a target in the same input or data-input.
- An output cannot contain targets. It can only contain pointers.

#### Example Script

The following is an example of a script to timestamp a box using the DApp described [here](https://www.ergoforum.org/t/a-trustless-timestamping-service-for-boxes/432/9?u=scalahub).
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
