module Main exposing (..)

import Browser
import Html exposing (..)
import Html.Events exposing (onInput)
import Yaml.Decode as Decode exposing (..)
import Yaml.Encode exposing (..)
import String exposing (..)
import Array exposing (..)

-- MAIN


main =
  Browser.element
    { init = init
    , update = update
    , subscriptions = \_ -> Sub.none
    , view = view
    }

-- MODEL


type alias Model = String


init : () -> (Model, Cmd Msg)
init _ = ( "", Cmd.none )



-- UPDATE


type Msg = YamlInput String


update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    YamlInput str ->
          ( str , Cmd.none)



-- VIEW


view : Model -> Html Msg
view model =
  div [] 
  [ textarea [ onInput YamlInput ] []
  , p [] 
    [ 
      case Decode.fromString Decode.value model of
        Ok val -> text "All good!"
        Err e -> 
          case e of
            Parsing s -> text <| "Parsing " ++ fromInt (Tuple.first <| getErrorLocation s) ++ ", " ++ fromInt (Tuple.second <| getErrorLocation s) ++ ": "++  getOffendingString model (getErrorLocation s)
            Decoding ss -> text <| "Decoding " ++ ss
      
    ]
  ]

getErrorLocation : String -> (Int, Int)
getErrorLocation errorStr =
  let
    defaultResult = (-1, -1)
  in
  case split ":" errorStr of
     (x :: xs) ->  
        case split "," x of
           (line :: col :: other) -> 
              (Maybe.withDefault -1 (toInt (dropLeft 5 line)), Maybe.withDefault -1 (toInt (dropLeft 8 col)))
           _ -> defaultResult
     _ -> defaultResult

getOffendingString : String -> (Int, Int) -> String
getOffendingString fullText (line, col) =
  case Array.get (line - 1) (Array.fromList (lines fullText)) of
    Just theLine -> (String.slice 0 col theLine) ++ " <-- Here" ++ theLine
    Nothing -> "Couldn't find the offending string"