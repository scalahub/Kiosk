# Assembler

Assembler is a Json-based scripting language for developing the offchain part of an Ergo DApp.

It allows us to specify a `Protocol`, which is a sequence of: 
- Constants
- Data-input box definitions
- Input box definitions 
- Output box definitions
- Unary operations
- Binary operations
- Conversions

## Declaration
A `Declaration` maps to an actual object containing a `Kiosktype[_]`.
For instance, an `Id` type maps to a `KioskCollByte` object (of size 32), 
while an `Address` type maps to a `KioskErgoTree` object.

We have three types of declarations:
- **Constants**: These specify initial values. Examples:  
  - `{"name":"myInt", "type":"int", "value":"123"}`
  - `{"name":"myAddress", "type":"address", "value":"9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"}`
- **Instructions**: These specify binary and unary operations and conversions. Example:
  - Binary Op: `{"name":"mySum", "first":"someValue", "op":"Add", "second":"someOtherValue"}`
  - Unary Op: `{"out":"myLong7", "in":"myLong2", "op":"Neg"}`
  - Conversion: `{"to":"myErgoTree", "from":"myGroupElement", "converter":"ProveDlog"}`
- **Box**: These are used to define boxes or search for boxes. There are four types: `Address`, `Id`, `Register`, and `Long` (see below).  

## Box Declarations
There are four type of box declarations:
- **Address**: The address of the box, internally stored as `KioskErgoTree`.
- **Id**: Box Id or token Id, internally stored as a `KioskCollByte` of size 32.
- **Register**: Register contents, internally stored as `KioskType[_]`.
- **Long**: NanoErgs or token quantity, internally stored as `KioskLong`.

#### Name and Value
A box declaration can contain exactly one of:
- A "name" (i.e., the declaration defines a new variable that will be referenced elsewhere), or
- A "value" (i.e., the declaration references another variable that is already defined elsewhere).

The exception is the `Long` type, which can have both name and value. The value in conjunction with a filter can be used to filter-out
boxes, and the name corresponds to the final value matched. 

The following give some examples:
1. `{"name":"myAddress"}`
2. `{"value":"myAddress"}`
3. `{"name":"actualNanoErgs", "value":"someMinValue", "filter":"Gt"}`
4. `{"name":"actualNanoErgs", "value":"someMinValue"}`

- The first defines the address `myAddress`.
- The second references that address.
- The third defines the value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs < someMinValue`. 
- The fourth defines the value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs != someMinValue`.

 
#### Targets and Pointers

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

